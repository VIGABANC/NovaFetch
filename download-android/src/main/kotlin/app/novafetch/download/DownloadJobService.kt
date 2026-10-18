package app.novafetch.download

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import app.novafetch.common.DownloadId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DownloadJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = mutableMapOf<Int, Job>()

    override fun onStartJob(params: JobParameters): Boolean {
        if (Build.VERSION.SDK_INT < 34) return false
        val rawId = params.extras.getString(UidtDownloadScheduler.KEY_DOWNLOAD_ID) ?: return false
        val host = application as? DownloadExecutionHost ?: return false
        val notification = DownloadNotificationFactory(this).create(rawId)
        setNotification(
            params,
            DownloadNotificationFactory.notificationId(rawId),
            notification,
            JOB_END_NOTIFICATION_POLICY_DETACH,
        )
        val job = scope.launch {
            val outcome = runCatching { host.executeDownload(DownloadId(rawId)) }
                .getOrDefault(DownloadExecutionOutcome.RETRY)
            jobFinished(params, outcome == DownloadExecutionOutcome.RETRY)
            running.remove(params.jobId)
        }
        running[params.jobId] = job
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running.remove(params.jobId)?.cancel()
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
