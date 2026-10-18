package app.novafetch.library

import app.novafetch.common.DownloadId
import java.nio.file.Files
import kotlin.coroutines.startCoroutine

private class FakeSharedMediaStore : SharedMediaStore {
    val rows = linkedMapOf<String, StoredMedia>()
    var insertCount = 0
    var writeCount = 0
    var commitCount = 0

    override suspend fun inspect(uri: String): StoredMedia? = rows[uri]

    override suspend fun insertPending(metadata: FinalMediaMetadata): StoredMedia {
        insertCount += 1
        return StoredMedia("content://media/$insertCount", isPending = true).also {
            rows[it.uri] = it
        }
    }

    override suspend fun write(uri: String, source: java.nio.file.Path) {
        check(Files.size(source) > 0)
        check(rows.containsKey(uri))
        writeCount += 1
    }

    override suspend fun commit(uri: String) {
        val existing = checkNotNull(rows[uri])
        rows[uri] = existing.copy(isPending = false)
        commitCount += 1
    }

    override suspend fun delete(uri: String) {
        rows.remove(uri)
    }
}

private fun <T> runSuspendPublisher(block: suspend () -> T): T {
    var holder: Result<T>? = null
    block.startCoroutine(object : kotlin.coroutines.Continuation<T> {
        override val context = kotlin.coroutines.EmptyCoroutineContext
        override fun resumeWith(result: Result<T>) { holder = result }
    })
    return holder!!.getOrThrow()
}

fun main() {
    val source = Files.createTempFile("novafetch-mediastore", ".part")
    Files.write(source, ByteArray(32) { 7 })
    val metadata = FinalMediaMetadata("clip.mp4", "video/mp4", "Movies/NovaFetch")
    val store = FakeSharedMediaStore()
    val publisher = MediaStoreSharedMediaPublisher(store)

    val pending1 = runSuspendPublisher { publisher.prepare(DownloadId("job-1"), metadata) }
    val first = runSuspendPublisher { publisher.finish(pending1, source) }
    check(first.uri == "content://media/1")
    check(store.insertCount == 1)
    check(store.writeCount == 1)
    check(store.commitCount == 1)

    // Different jobs with the same display name must receive distinct MediaStore rows.
    val pending2 = runSuspendPublisher { publisher.prepare(DownloadId("job-duplicate"), metadata) }
    val duplicateName = runSuspendPublisher { publisher.finish(pending2, source) }
    check(duplicateName.uri != first.uri)
    check(store.insertCount == 2)

    // Crash recovery uses the exact persisted URI, not a filename lookup.
    val pending3 = runSuspendPublisher { publisher.prepare(DownloadId("job-recover"), metadata) }
    val recovered = runSuspendPublisher { publisher.resume(pending3.uri, source) }
    check(recovered?.uri == pending3.uri)
    check(store.rows[pending3.uri]?.isPending == false)

    // A crash after commit is idempotent and does not rewrite the media.
    val writesBefore = store.writeCount
    val commitsBefore = store.commitCount
    val alreadyPublished = runSuspendPublisher { publisher.resume(first.uri, source) }
    check(alreadyPublished == first)
    check(store.writeCount == writesBefore)
    check(store.commitCount == commitsBefore)

    println("MediaStoreSharedMediaPublisherTest: PASS")
}
