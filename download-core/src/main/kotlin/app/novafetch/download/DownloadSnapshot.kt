package app.novafetch.download

import app.novafetch.common.DownloadId
import app.novafetch.common.DownloadState

data class DownloadSnapshot(
    val id: DownloadId,
    val plan: DownloadPlan,
    val state: DownloadState,
    val downloadedBytes: Long = 0,
    val totalBytes: Long? = null,
    val tempPath: String? = null,
    val etag: String? = null,
    val lastModified: String? = null,
    val supportsResume: Boolean = false,
    val outputUri: String? = null,
    val retryCount: Int = 0,
    val failureCode: String? = null,
)
