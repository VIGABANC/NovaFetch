package app.novafetch.library

import app.novafetch.common.DownloadId
import app.novafetch.common.DownloadState
import app.novafetch.download.DownloadPlan
import app.novafetch.download.DownloadRepository
import app.novafetch.download.DownloadSnapshot
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlinx.coroutines.flow.Flow

private fun <T> runSuspendFinalizer(block: suspend () -> T): T {
    var holder: Result<T>? = null
    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<T>) { holder = result }
    })
    return holder!!.getOrThrow()
}

private class FinalizerRepository(initial: DownloadSnapshot) : DownloadRepository {
    var current = initial
    val history = mutableListOf(initial)

    override suspend fun create(snapshot: DownloadSnapshot) { current = snapshot; history += snapshot }
    override suspend fun get(id: DownloadId): DownloadSnapshot? = current.takeIf { it.id == id }
    override suspend fun update(snapshot: DownloadSnapshot) { current = snapshot; history += snapshot }
    override suspend fun unfinished(): List<DownloadSnapshot> = listOf(current)
    override fun observe(id: DownloadId): Flow<DownloadSnapshot> = object : Flow<DownloadSnapshot> {}
}

private class FakeSharedMediaPublisher : SharedMediaPublisher {
    val published = linkedMapOf<String, PublishedMedia>()
    var prepareCalls = 0
    var finishCalls = 0
    var resumeCalls = 0

    override suspend fun prepare(jobId: DownloadId, metadata: FinalMediaMetadata): PendingMedia {
        prepareCalls += 1
        return PendingMedia("content://media/${jobId.value}")
    }

    override suspend fun finish(pending: PendingMedia, source: Path): PublishedMedia {
        finishCalls += 1
        return PublishedMedia(pending.uri).also { published[pending.uri] = it }
    }

    override suspend fun resume(uri: String, source: Path): PublishedMedia? {
        resumeCalls += 1
        return published[uri]
    }
}



private class FakeMediaLibraryRepository : MediaLibraryRepository {
    val items = linkedMapOf<String, MediaItem>()

    override suspend fun upsert(item: MediaItem) {
        items[item.id] = item
    }

    override suspend fun getByDownloadId(downloadId: DownloadId): MediaItem? =
        items.values.firstOrNull { it.downloadId == downloadId }

    override fun observeAll(): Flow<List<MediaItem>> = object : Flow<List<MediaItem>> {}
}

private fun snapshotFor(
    id: String,
    path: Path,
    state: DownloadState = DownloadState.VERIFYING,
    totalBytes: Long = Files.size(path),
    outputUri: String? = null,
): DownloadSnapshot = DownloadSnapshot(
    id = DownloadId(id),
    plan = DownloadPlan.DirectPlan("https://example.test/$id.mp4", "video/mp4", "$id.mp4"),
    state = state,
    downloadedBytes = Files.size(path),
    totalBytes = totalBytes,
    tempPath = path.toString(),
    outputUri = outputUri,
)

fun main() {
    val dir = Files.createTempDirectory("novafetch-finalizer-test")
    val file = dir.resolve("good.part")
    Files.write(file, ByteArray(4096) { (it % 251).toByte() })
    val repo = FinalizerRepository(snapshotFor("good", file))
    val publisher = FakeSharedMediaPublisher()
    val library = FakeMediaLibraryRepository()
    val finalizer = MediaFinalizer(
        repository = repo,
        verifier = BasicFileIntegrityVerifier(),
        publisher = publisher,
        libraryRepository = library,
        nowEpochMs = { 1_234L },
    )
    val metadata = FinalMediaMetadata("good.mp4", "video/mp4", "Movies/NovaFetch")

    val completed = runSuspendFinalizer { finalizer.finalize(DownloadId("good"), metadata) }
    check(completed.state == DownloadState.COMPLETED)
    check(completed.outputUri == "content://media/good")
    check(repo.history.any { it.state == DownloadState.FINALIZING })
    check(publisher.prepareCalls == 1)
    check(publisher.finishCalls == 1)
    val libraryItem = library.items.values.single()
    check(libraryItem.downloadId == DownloadId("good"))
    check(libraryItem.contentUri == "content://media/good")
    check(libraryItem.mediaType == LibraryMediaType.VIDEO)
    check(libraryItem.mimeType == "video/mp4")
    check(libraryItem.fileSize == 4096L)
    check(libraryItem.createdAtEpochMs == 1_234L)

    val repeated = runSuspendFinalizer { finalizer.finalize(DownloadId("good"), metadata) }
    check(repeated.state == DownloadState.COMPLETED)
    check(publisher.prepareCalls == 1)
    check(publisher.finishCalls == 1)
    check(library.items.size == 1)

    val badFile = dir.resolve("bad.part")
    Files.write(badFile, ByteArray(64))
    val badRepo = FinalizerRepository(snapshotFor("bad", badFile, totalBytes = 128))
    val badPublisher = FakeSharedMediaPublisher()
    val badLibrary = FakeMediaLibraryRepository()
    val badFinalizer = MediaFinalizer(
        badRepo, BasicFileIntegrityVerifier(), badPublisher, badLibrary, { 2_000L },
    )
    val failed = runSuspendFinalizer { badFinalizer.finalize(DownloadId("bad"), metadata) }
    check(failed.state == DownloadState.FAILED)
    check(failed.failureCode == "SIZE_MISMATCH")
    check(badPublisher.prepareCalls == 0)
    check(badPublisher.finishCalls == 0)

    // Crash-recovery path: file was already published but Room still says FINALIZING.
    val recoveryUri = "content://media/recover"
    val recoveryRepo = FinalizerRepository(
        snapshotFor("recover", file, state = DownloadState.FINALIZING, outputUri = recoveryUri),
    )
    val recoveryPublisher = FakeSharedMediaPublisher().apply {
        published[recoveryUri] = PublishedMedia(recoveryUri)
    }
    val recoveryLibrary = FakeMediaLibraryRepository()
    val recoveryFinalizer = MediaFinalizer(
        recoveryRepo, BasicFileIntegrityVerifier(), recoveryPublisher, recoveryLibrary, { 3_000L },
    )
    val recovered = runSuspendFinalizer { recoveryFinalizer.finalize(DownloadId("recover"), metadata) }
    check(recovered.state == DownloadState.COMPLETED)
    check(recovered.outputUri == "content://media/recover")
    check(recoveryPublisher.prepareCalls == 0)
    check(recoveryPublisher.finishCalls == 0)
    check(recoveryPublisher.resumeCalls == 1)
    check(recoveryLibrary.items.values.single().contentUri == "content://media/recover")

    println("MediaFinalizerTest: PASS")
}
