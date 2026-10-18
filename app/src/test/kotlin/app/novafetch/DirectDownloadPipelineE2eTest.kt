package app.novafetch

import app.novafetch.common.DownloadId
import app.novafetch.common.DownloadState
import app.novafetch.download.DirectDownloadRunner
import app.novafetch.download.DownloadPlan
import app.novafetch.download.DownloadRepository
import app.novafetch.download.DownloadSnapshot
import app.novafetch.download.DownloadTempPathProvider
import app.novafetch.download.UrlConnectionRangeProbe
import app.novafetch.download.UrlConnectionTransferEngine
import app.novafetch.library.BasicFileIntegrityVerifier
import app.novafetch.library.FinalMediaMetadata
import app.novafetch.library.MediaFinalizer
import app.novafetch.library.MediaItem
import app.novafetch.library.MediaLibraryRepository
import app.novafetch.library.PublishedMedia
import app.novafetch.library.SharedMediaPublisher
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlinx.coroutines.flow.Flow

private fun <T> runSuspendE2e(block: suspend () -> T): T {
    var holder: Result<T>? = null
    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<T>) { holder = result }
    })
    return holder!!.getOrThrow()
}

private class E2eDownloadRepository(initial: DownloadSnapshot) : DownloadRepository {
    var current = initial
    override suspend fun create(snapshot: DownloadSnapshot) { current = snapshot }
    override suspend fun get(id: DownloadId): DownloadSnapshot? = current.takeIf { it.id == id }
    override suspend fun update(snapshot: DownloadSnapshot) { current = snapshot }
    override suspend fun unfinished(): List<DownloadSnapshot> = listOf(current)
    override fun observe(id: DownloadId): Flow<DownloadSnapshot> = object : Flow<DownloadSnapshot> {}
}

private class E2eLibraryRepository : MediaLibraryRepository {
    val items = linkedMapOf<String, MediaItem>()
    override suspend fun upsert(item: MediaItem) { items[item.id] = item }
    override suspend fun getByDownloadId(downloadId: DownloadId): MediaItem? =
        items.values.firstOrNull { it.downloadId == downloadId }
    override fun observeAll(): Flow<List<MediaItem>> = object : Flow<List<MediaItem>> {}
}

private class CopyingPublisher(private val finalDir: Path) : SharedMediaPublisher {
    val destinations = linkedMapOf<String, Path>()

    override suspend fun findPublished(jobId: DownloadId): PublishedMedia? =
        destinations[jobId.value]?.let { PublishedMedia(it.toUri().toString()) }

    override suspend fun publish(
        jobId: DownloadId,
        source: Path,
        metadata: FinalMediaMetadata,
    ): PublishedMedia {
        Files.createDirectories(finalDir)
        val destination = finalDir.resolve(metadata.displayName)
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
        destinations[jobId.value] = destination
        return PublishedMedia(destination.toUri().toString())
    }
}

fun main() {
    val base = System.getenv("TEST_MEDIA_BASE_URL") ?: error("TEST_MEDIA_BASE_URL required")
    val workDir = Files.createTempDirectory("novafetch-pipeline-e2e")
    val tempDir = workDir.resolve("temp")
    val finalDir = workDir.resolve("final")
    val id = DownloadId("pipeline-playable")
    val repository = E2eDownloadRepository(
        DownloadSnapshot(
            id = id,
            plan = DownloadPlan.DirectPlan(
                sourceUrl = "$base/playable.mp4",
                expectedMime = "video/mp4",
                suggestedFileName = "playable.mp4",
            ),
            state = DownloadState.QUEUED,
        ),
    )
    val runner = DirectDownloadRunner(
        repository = repository,
        transferEngine = UrlConnectionTransferEngine(UrlConnectionRangeProbe()),
        tempPathProvider = DownloadTempPathProvider { _, fileName -> tempDir.resolve("$fileName.part") },
    )

    val downloaded = runSuspendE2e { runner.run(id) }
    check(downloaded.state == DownloadState.VERIFYING)

    val library = E2eLibraryRepository()
    val publisher = CopyingPublisher(finalDir)
    val finalizer = MediaFinalizer(
        repository = repository,
        verifier = BasicFileIntegrityVerifier(),
        publisher = publisher,
        libraryRepository = library,
        nowEpochMs = { 10_000L },
    )
    val completed = runSuspendE2e {
        finalizer.finalize(
            id,
            FinalMediaMetadata("playable.mp4", "video/mp4", "Movies/NovaFetch"),
        )
    }

    check(completed.state == DownloadState.COMPLETED)
    val finalFile = publisher.destinations.getValue(id.value)
    check(Files.isRegularFile(finalFile))
    check(library.items.getValue(id.value).contentUri == finalFile.toUri().toString())

    val probe = ProcessBuilder(
        "ffprobe", "-v", "error",
        "-show_entries", "stream=codec_type,codec_name",
        "-of", "compact",
        finalFile.toString(),
    ).redirectErrorStream(true).start()
    val probeOutput = probe.inputStream.bufferedReader().readText()
    check(probe.waitFor() == 0) { probeOutput }
    check("codec_type=video" in probeOutput) { probeOutput }
    check("codec_type=audio" in probeOutput) { probeOutput }

    println("DirectDownloadPipelineE2eTest: PASS")
}
