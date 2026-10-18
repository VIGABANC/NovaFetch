package app.novafetch.data

import app.novafetch.download.DownloadSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomDownloadListRepository(
    private val dao: DownloadDao,
) {
    fun observeAll(): Flow<List<DownloadSnapshot>> =
        dao.observeAllJobs().map { jobs -> jobs.map(DownloadJobEntity::toSnapshot) }
}
