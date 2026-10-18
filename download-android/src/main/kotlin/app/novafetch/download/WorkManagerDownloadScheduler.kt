package app.novafetch.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.novafetch.common.DownloadId

class WorkManagerDownloadScheduler(private val context: Context) : PlatformDownloadScheduler {
    override suspend fun schedule(id: DownloadId, estimatedBytes: Long?): ScheduleResult {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setInputData(workDataOf(DownloadWorker.KEY_DOWNLOAD_ID to id.value))
            .addTag(tag(id))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName(id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return ScheduleResult.Enqueued(SchedulerRoute.WORK_MANAGER)
    }

    override suspend fun cancel(id: DownloadId) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(id))
    }

    private fun uniqueName(id: DownloadId) = "novafetch-download-${id.value}"
    private fun tag(id: DownloadId) = "download:${id.value}"
}
