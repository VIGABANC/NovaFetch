package app.novafetch.download

import android.app.ServiceInfo
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import app.novafetch.common.DownloadId

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val rawId = inputData.getString(KEY_DOWNLOAD_ID) ?: return Result.failure()
        setForeground(createForegroundInfo(rawId))
        val host = applicationContext as? DownloadExecutionHost ?: return Result.retry()
        return when (host.executeDownload(DownloadId(rawId))) {
            DownloadExecutionOutcome.COMPLETED -> Result.success()
            DownloadExecutionOutcome.RETRY -> Result.retry()
            DownloadExecutionOutcome.FAILED -> Result.failure()
        }
    }

    private fun createForegroundInfo(rawId: String): ForegroundInfo = ForegroundInfo(
        DownloadNotificationFactory.notificationId(rawId),
        DownloadNotificationFactory(applicationContext).create(rawId),
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
    }
}
