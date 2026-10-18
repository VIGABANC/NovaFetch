package app.novafetch.download

import app.novafetch.common.DownloadId
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    suspend fun create(snapshot: DownloadSnapshot)
    suspend fun get(id: DownloadId): DownloadSnapshot?
    suspend fun update(snapshot: DownloadSnapshot)
    suspend fun unfinished(): List<DownloadSnapshot>
    fun observe(id: DownloadId): Flow<DownloadSnapshot>
}
