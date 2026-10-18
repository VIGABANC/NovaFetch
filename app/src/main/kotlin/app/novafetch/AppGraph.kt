package app.novafetch

import android.content.Context
import android.os.Build
import app.novafetch.browser.BrowserSessionManager
import app.novafetch.common.DownloadId
import app.novafetch.data.NovaFetchDatabaseFactory
import app.novafetch.data.RoomDownloadListRepository
import app.novafetch.data.RoomDownloadRepository
import app.novafetch.data.RoomMediaLibraryRepository
import app.novafetch.download.AdaptiveDownloadScheduler
import app.novafetch.download.DirectDownloadRunner
import app.novafetch.download.DownloadCoordinator
import app.novafetch.download.DownloadSchedulerPolicy
import app.novafetch.download.DownloadStateMachine
import app.novafetch.download.DownloadTempPathProvider
import app.novafetch.download.UidtDownloadScheduler
import app.novafetch.download.UrlConnectionRangeProbe
import app.novafetch.download.UrlConnectionTransferEngine
import app.novafetch.download.WorkManagerDownloadScheduler
import app.novafetch.download.DownloadPlan
import app.novafetch.library.AndroidSharedMediaStore
import app.novafetch.library.BasicFileIntegrityVerifier
import app.novafetch.library.MediaFinalizer
import app.novafetch.library.MediaStoreSharedMediaPublisher
import java.net.URI

class AppGraph(private val context: Context) {
    val browserManager by lazy { BrowserSessionManager(context) }
    private val database = NovaFetchDatabaseFactory.create(context)

    val downloadRepository = RoomDownloadRepository(database.downloadDao())
    val downloadListRepository = RoomDownloadListRepository(database.downloadDao())
    val mediaLibraryRepository = RoomMediaLibraryRepository(database.mediaLibraryDao())

    private val scheduler = AdaptiveDownloadScheduler(
        apiLevel = { Build.VERSION.SDK_INT },
        policy = DownloadSchedulerPolicy(),
        uidtScheduler = UidtDownloadScheduler(context),
        workManagerScheduler = WorkManagerDownloadScheduler(context),
    )

    val coordinator = DownloadCoordinator(
        repository = downloadRepository,
        scheduler = scheduler,
        stateMachine = DownloadStateMachine(),
    )

    private val runner = DirectDownloadRunner(
        repository = downloadRepository,
        transferEngine = UrlConnectionTransferEngine(UrlConnectionRangeProbe()),
        tempPathProvider = DownloadTempPathProvider { id, fileName ->
            val safeName = fileName.substringAfterLast('/').substringAfterLast('\\').ifBlank { "download.bin" }
            context.cacheDir.toPath()
                .resolve("downloads")
                .resolve(id.value)
                .resolve("$safeName.part")
        },
    )

    private val finalizer = MediaFinalizer(
        repository = downloadRepository,
        verifier = BasicFileIntegrityVerifier(),
        publisher = MediaStoreSharedMediaPublisher(AndroidSharedMediaStore(context.contentResolver)),
        libraryRepository = mediaLibraryRepository,
    )

    val executionHost = DirectDownloadExecutionHost(
        repository = downloadRepository,
        runner = runner,
        finalizer = finalizer,
        metadataFactory = FinalMediaMetadataFactory(),
    )

    suspend fun enqueueDirect(url: String): DownloadId {
        val plan = directPlan(url)
        return coordinator.enqueue(plan)
    }

    private fun directPlan(url: String): DownloadPlan.DirectPlan {
        val uri = URI(url)
        require(uri.scheme == "http" || uri.scheme == "https") { "Only HTTP(S) URLs are supported" }
        val fileName = uri.path.substringAfterLast('/').ifBlank { "download.bin" }
        val mime = when (fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
            "mp4", "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/aac"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> null
        }
        return DownloadPlan.DirectPlan(url, mime, fileName)
    }
}
