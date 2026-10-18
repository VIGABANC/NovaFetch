package app.novafetch.data

import app.novafetch.common.DownloadId
import app.novafetch.library.LibraryMediaType
import app.novafetch.library.MediaItem
import app.novafetch.library.MediaLibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

private fun <T> runSuspendLibraryData(block: suspend () -> T): T {
    var holder: Result<T>? = null
    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<T>) { holder = result }
    })
    return holder!!.getOrThrow()
}

private class FakeMediaLibraryDao : MediaLibraryDao {
    val rows = linkedMapOf<String, MediaItemEntity>()

    override suspend fun upsert(item: MediaItemEntity) { rows[item.id] = item }
    override suspend fun getByDownloadId(downloadId: String): MediaItemEntity? =
        rows.values.firstOrNull { it.downloadId == downloadId }
    override fun observeAll(): Flow<List<MediaItemEntity>> = object : Flow<List<MediaItemEntity>> {}
}

fun main() {
    val dao = FakeMediaLibraryDao()
    val repository: MediaLibraryRepository = RoomMediaLibraryRepository(dao)
    val item = MediaItem(
        id = "download-1",
        downloadId = DownloadId("download-1"),
        title = "Example.mp4",
        mediaType = LibraryMediaType.VIDEO,
        mimeType = "video/mp4",
        contentUri = "content://media/video/1",
        fileSize = 42_000L,
        createdAtEpochMs = 1_234L,
    )

    runSuspendLibraryData { repository.upsert(item) }
    val row = dao.rows.getValue(item.id)
    check(row.downloadId == "download-1")
    check(row.mediaType == "VIDEO")
    check(row.contentUri == item.contentUri)

    val loaded = runSuspendLibraryData { repository.getByDownloadId(item.downloadId) }
    check(loaded == item)

    repository.observeAll()
    println("RoomMediaLibraryRepositoryTest: PASS")
}
