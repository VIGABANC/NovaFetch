package app.novafetch.data

import app.novafetch.common.DownloadId
import app.novafetch.download.DownloadRepository
import app.novafetch.download.DownloadSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomDownloadRepository(
    private val dao: DownloadDao,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) : DownloadRepository {
    override suspend fun create(snapshot: DownloadSnapshot) {
        val now = nowEpochMs()
        val fileName = (snapshot.plan as app.novafetch.download.DownloadPlan.DirectPlan).suggestedFileName
        dao.upsertJob(
            snapshot.toEntity(
                title = fileName,
                tempPath = null,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
    }

    override suspend fun get(id: DownloadId): DownloadSnapshot? =
        dao.getJob(id.value)?.toSnapshot()

    override suspend fun update(snapshot: DownloadSnapshot) {
        val existing = requireNotNull(dao.getJob(snapshot.id.value)) {
            "unknown download: ${snapshot.id.value}"
        }
        dao.upsertJob(
            existing.copy(
                downloadedBytes = snapshot.downloadedBytes,
                totalBytes = snapshot.totalBytes,
                tempPath = snapshot.tempPath,
                supportsResume = snapshot.supportsResume,
                etag = snapshot.etag,
                lastModified = snapshot.lastModified,
                outputUri = snapshot.outputUri,
                state = snapshot.state.name,
                retryCount = snapshot.retryCount,
                failureCode = snapshot.failureCode,
                updatedAtEpochMs = nowEpochMs(),
            ),
        )
    }

    override suspend fun unfinished(): List<DownloadSnapshot> =
        dao.unfinishedJobs().map(DownloadJobEntity::toSnapshot)

    override fun observe(id: DownloadId): Flow<DownloadSnapshot> =
        dao.observeJob(id.value).map { entity ->
            requireNotNull(entity) { "unknown download: ${id.value}" }.toSnapshot()
        }
}
