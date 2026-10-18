package app.novafetch.download

import app.novafetch.common.DownloadId

interface DownloadScheduler {
    suspend fun schedule(id: DownloadId)
    suspend fun cancel(id: DownloadId)
}
