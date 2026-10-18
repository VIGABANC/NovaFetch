package app.novafetch.download

import app.novafetch.common.DownloadId

enum class DownloadExecutionOutcome {
    COMPLETED,
    RETRY,
    FAILED,
}

interface DownloadExecutionHost {
    suspend fun executeDownload(id: DownloadId): DownloadExecutionOutcome
}
