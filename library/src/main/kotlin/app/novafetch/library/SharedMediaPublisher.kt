package app.novafetch.library

import app.novafetch.common.DownloadId
import java.nio.file.Path

data class PendingMedia(val uri: String) {
    init { require(uri.isNotBlank()) }
}

data class PublishedMedia(val uri: String) {
    init { require(uri.isNotBlank()) }
}

interface SharedMediaPublisher {
    /** Create a new job-owned pending destination. This must never reuse another job's row. */
    suspend fun prepare(jobId: DownloadId, metadata: FinalMediaMetadata): PendingMedia

    /** Write and publish a destination previously returned by [prepare]. */
    suspend fun finish(pending: PendingMedia, source: Path): PublishedMedia

    /** Recover a destination by its exact persisted URI after process death. */
    suspend fun resume(uri: String, source: Path): PublishedMedia?
}
