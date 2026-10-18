package app.novafetch.data

import app.novafetch.common.DownloadId
import app.novafetch.common.DownloadState
import app.novafetch.download.DownloadPlan
import app.novafetch.download.DownloadSnapshot
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

private fun <T> runSuspendData(block: suspend () -> T): T {
    var holder: Result<T>? = null
    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<T>) { holder = result }
    })
    return holder!!.getOrThrow()
}

private class FakeDownloadDao : DownloadDao {
    val jobs = linkedMapOf<String, DownloadJobEntity>()
    val parts = linkedMapOf<String, DownloadPartEntity>()

    override suspend fun upsertJob(job: DownloadJobEntity) { jobs[job.id] = job }
    override suspend fun upsertPart(part: DownloadPartEntity) { parts[part.id] = part }
    override suspend fun getJob(id: String): DownloadJobEntity? = jobs[id]
    override fun observeJob(id: String): Flow<DownloadJobEntity?> = object : Flow<DownloadJobEntity?> {}
    override suspend fun unfinishedJobs(): List<DownloadJobEntity> = jobs.values.filter {
        it.state !in setOf("COMPLETED", "FAILED", "CANCELLED")
    }
    override suspend fun updateProgress(id: String, downloadedBytes: Long, state: String, updatedAtEpochMs: Long) {
        jobs[id] = requireNotNull(jobs[id]).copy(
            downloadedBytes = downloadedBytes,
            state = state,
            updatedAtEpochMs = updatedAtEpochMs,
        )
    }
    override suspend fun partsForJob(downloadJobId: String): List<DownloadPartEntity> =
        parts.values.filter { it.downloadJobId == downloadJobId }.sortedBy { it.partIndex }
}

fun main() {
    val dao = FakeDownloadDao()
    val repo = RoomDownloadRepository(dao, nowEpochMs = { 1234L })
    val id = DownloadId("repo-job")
    val plan = DownloadPlan.DirectPlan("https://example.test/a.mp4", "video/mp4", "a.mp4")
    val queued = DownloadSnapshot(id, plan, DownloadState.QUEUED)

    runSuspendData { repo.create(queued) }
    check(dao.jobs.getValue(id.value).state == "QUEUED")
    check(dao.jobs.getValue(id.value).createdAtEpochMs == 1234L)

    val loaded = runSuspendData { repo.get(id) }
    check(loaded == queued)

    val downloading = queued.copy(
        state = DownloadState.DOWNLOADING,
        downloadedBytes = 777,
        totalBytes = 65_536,
        tempPath = "/private/repo-job.part",
        etag = "\"etag-repo\"",
        lastModified = "Sat, 17 Oct 2026 00:00:00 GMT",
        supportsResume = true,
    )
    runSuspendData { repo.update(downloading) }
    val persisted = dao.jobs.getValue(id.value)
    check(persisted.state == "DOWNLOADING")
    check(persisted.downloadedBytes == 777L)
    check(persisted.totalBytes == 65_536L)
    check(persisted.tempPath == "/private/repo-job.part")
    check(persisted.etag == "\"etag-repo\"")
    check(persisted.lastModified == "Sat, 17 Oct 2026 00:00:00 GMT")
    check(persisted.supportsResume)
    check(persisted.title == "a.mp4")

    val unfinished = runSuspendData { repo.unfinished() }
    check(unfinished.single() == downloading)

    val completed = downloading.copy(
        state = DownloadState.COMPLETED,
        outputUri = "content://media/external/video/repo-job",
    )
    runSuspendData { repo.update(completed) }
    check(dao.jobs.getValue(id.value).outputUri == completed.outputUri)
    check(runSuspendData { repo.unfinished() }.isEmpty())

    println("RoomDownloadRepositoryTest: PASS")
}
