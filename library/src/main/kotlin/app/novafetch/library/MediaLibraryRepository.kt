package app.novafetch.library

import app.novafetch.common.DownloadId
import kotlinx.coroutines.flow.Flow

interface MediaLibraryRepository {
    suspend fun upsert(item: MediaItem)
    suspend fun getByDownloadId(downloadId: DownloadId): MediaItem?
    fun observeAll(): Flow<List<MediaItem>>
}
