package app.novafetch.download

import app.novafetch.common.DownloadId

interface DownloadScheduler {
    suspend fun schedule(id: DownloadId)

    suspend fun scheduleRecovery(id: DownloadId) {
        schedule(id)
    }

    suspend fun cancel(id: DownloadId)
}
