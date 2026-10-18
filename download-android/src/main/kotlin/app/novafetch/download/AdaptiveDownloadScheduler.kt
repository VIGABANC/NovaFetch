package app.novafetch.download

import app.novafetch.common.DownloadId

sealed interface ScheduleResult {
    data class Enqueued(val route: SchedulerRoute) : ScheduleResult
    data class Rejected(val reason: String) : ScheduleResult
}

interface PlatformDownloadScheduler {
    suspend fun schedule(id: DownloadId, estimatedBytes: Long?): ScheduleResult
    suspend fun cancel(id: DownloadId)
}

class AdaptiveDownloadScheduler(
    private val apiLevel: () -> Int,
    private val policy: DownloadSchedulerPolicy,
    private val uidtScheduler: PlatformDownloadScheduler,
    private val workManagerScheduler: PlatformDownloadScheduler,
) : DownloadScheduler {
    override suspend fun schedule(id: DownloadId) {
        scheduleWithEstimate(id, null)
    }

    suspend fun scheduleWithEstimate(id: DownloadId, estimatedBytes: Long?): ScheduleResult =
        when (policy.select(apiLevel(), userPaused = false)) {
            SchedulerRoute.UIDT -> uidtScheduler.schedule(id, estimatedBytes)
            SchedulerRoute.WORK_MANAGER -> workManagerScheduler.schedule(id, estimatedBytes)
            SchedulerRoute.NONE -> ScheduleResult.Rejected("download is paused")
        }

    override suspend fun scheduleRecovery(id: DownloadId) {
        workManagerScheduler.schedule(id, null)
    }

    override suspend fun cancel(id: DownloadId) {
        uidtScheduler.cancel(id)
        workManagerScheduler.cancel(id)
    }
}
