package app.novafetch.data

import app.novafetch.common.DownloadId
import app.novafetch.library.MediaItem
import app.novafetch.library.MediaLibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMediaLibraryRepository(
    private val dao: MediaLibraryDao,
) : MediaLibraryRepository {
    override suspend fun upsert(item: MediaItem) = dao.upsert(item.toEntity())

    override suspend fun getByDownloadId(downloadId: DownloadId): MediaItem? =
        dao.getByDownloadId(downloadId.value)?.toMediaItem()

    override fun observeAll(): Flow<List<MediaItem>> =
        dao.observeAll().map { items -> items.map(MediaItemEntity::toMediaItem) }
}
