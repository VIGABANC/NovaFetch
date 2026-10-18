package app.novafetch.download

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.PersistableBundle
import app.novafetch.common.DownloadId

class UidtDownloadScheduler(private val context: Context) : PlatformDownloadScheduler {
    override suspend fun schedule(id: DownloadId, estimatedBytes: Long?): ScheduleResult {
        if (Build.VERSION.SDK_INT < 34) {
            return ScheduleResult.Rejected("UIDT requires API 34+")
        }

        val extras = PersistableBundle().apply {
            putString(KEY_DOWNLOAD_ID, id.value)
        }
        val requiredNetwork = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val builder = JobInfo.Builder(
            stableJobId(id),
            ComponentName(context, DownloadJobService::class.java),
        )
            .setRequiredNetwork(requiredNetwork)
            .setUserInitiated(true)
            .setExtras(extras)

        if (estimatedBytes != null && estimatedBytes > 0L) {
            builder.setEstimatedNetworkBytes(estimatedBytes, 0L)
        }

        val scheduler = context.getSystemService(JobScheduler::class.java)
        return if (scheduler.schedule(builder.build()) == JobScheduler.RESULT_SUCCESS) {
            ScheduleResult.Enqueued(SchedulerRoute.UIDT)
        } else {
            ScheduleResult.Rejected("JobScheduler rejected UIDT job")
        }
    }

    override suspend fun cancel(id: DownloadId) {
        if (Build.VERSION.SDK_INT >= 34) {
            context.getSystemService(JobScheduler::class.java).cancel(stableJobId(id))
        }
    }

    private fun stableJobId(id: DownloadId): Int =
        (id.value.hashCode() and Int.MAX_VALUE).coerceAtLeast(1)

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
    }
}
