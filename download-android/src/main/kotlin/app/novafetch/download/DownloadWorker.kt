package app.novafetch.download

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import app.novafetch.common.DownloadId
import java.io.IOException
import java.util.concurrent.CancellationException

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val rawId = inputData.getString(KEY_DOWNLOAD_ID) ?: return Result.failure()
        setForeground(createForegroundInfo(rawId))
        val host = applicationContext as? DownloadExecutionHost ?: return Result.retry()
        return try {
            when (host.executeDownload(DownloadId(rawId))) {
                DownloadExecutionOutcome.COMPLETED -> Result.success()
                DownloadExecutionOutcome.RETRY -> Result.retry()
                DownloadExecutionOutcome.FAILED -> Result.failure()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            if (runAttemptCount < MAX_DIRECT_TRANSFER_RETRIES) Result.retry() else Result.failure()
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
