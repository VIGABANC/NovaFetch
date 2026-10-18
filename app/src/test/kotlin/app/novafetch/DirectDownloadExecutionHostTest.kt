package app.novafetch

import app.novafetch.common.DownloadId
import app.novafetch.common.DownloadState
import app.novafetch.download.*
import app.novafetch.library.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlinx.coroutines.flow.Flow

private fun <T> runSuspendHost(block: suspend () -> T): T {
    var holder: Result<T>? = null
    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<T>) { holder = result }
    })
    return holder!!.getOrThrow()
}

private class HostRepo(initial: DownloadSnapshot) : DownloadRepository {
    var current = initial
    override suspend fun create(snapshot: DownloadSnapshot) { current = snapshot }
    override suspend fun get(id: DownloadId): DownloadSnapshot? = current.takeIf { it.id == id }
    override suspend fun update(snapshot: DownloadSnapshot) { current = snapshot }
    override suspend fun unfinished(): List<DownloadSnapshot> = listOf(current)
    override fun observe(id: DownloadId): Flow<DownloadSnapshot> = object : Flow<DownloadSnapshot> {}
}

private class HostLibrary : MediaLibraryRepository {
    val items = mutableMapOf<String, MediaItem>()
    override suspend fun upsert(item: MediaItem) { items[item.id] = item }
    override suspend fun getByDownloadId(downloadId: DownloadId): MediaItem? = items.values.firstOrNull { it.downloadId == downloadId }
    override fun observeAll(): Flow<List<MediaItem>> = object : Flow<List<MediaItem>> {}
}

private class HostPublisher(private val dir: Path) : SharedMediaPublisher {
    private val byUri = mutableMapOf<String, Path>()

    override suspend fun prepare(jobId: DownloadId, metadata: FinalMediaMetadata): PendingMedia {
        Files.createDirectories(dir)
        val target = dir.resolve("${jobId.value}-${metadata.displayName}")
        val uri = target.toUri().toString()
        byUri[uri] = target
        return PendingMedia(uri)
    }

    override suspend fun finish(pending: PendingMedia, source: Path): PublishedMedia {
        val target = checkNotNull(byUri[pending.uri])
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
        return PublishedMedia(pending.uri)
    }

    override suspend fun resume(uri: String, source: Path): PublishedMedia? {
        val target = byUri[uri] ?: return null
        if (!Files.exists(target)) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
        return PublishedMedia(uri)
    }
}


fun main() {
    val base = System.getenv("TEST_MEDIA_BASE_URL") ?: error("TEST_MEDIA_BASE_URL required")
    val root = Files.createTempDirectory("novafetch-host-test")
    val id = DownloadId("host-playable")
    val repo = HostRepo(
        DownloadSnapshot(
            id = id,
            plan = DownloadPlan.DirectPlan("$base/playable.mp4", "video/mp4", "host-playable.mp4"),
            state = DownloadState.QUEUED,
        ),
    )
    val library = HostLibrary()
    val runner = DirectDownloadRunner(
        repo,
        UrlConnectionTransferEngine(UrlConnectionRangeProbe()),
        DownloadTempPathProvider { _, name -> root.resolve("temp/$name.part") },
    )
    val finalizer = MediaFinalizer(
        repo,
        BasicFileIntegrityVerifier(),
        HostPublisher(root.resolve("final")),
        library,
    )
    val host = DirectDownloadExecutionHost(repo, runner, finalizer, FinalMediaMetadataFactory())

    val outcome = runSuspendHost { host.executeDownload(id) }
    check(outcome == DownloadExecutionOutcome.COMPLETED)
    check(repo.current.state == DownloadState.COMPLETED)
    check(library.items[id.value]?.contentUri?.isNotBlank() == true)

    println("DirectDownloadExecutionHostTest: PASS")
}
