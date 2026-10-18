package app.novafetch.library

import app.novafetch.common.DownloadId
import java.nio.file.Path

data class PublishedMedia(val uri: String) {
    init { require(uri.isNotBlank()) }
}

interface SharedMediaPublisher {
    suspend fun findPublished(jobId: DownloadId): PublishedMedia?

    suspend fun publish(
        jobId: DownloadId,
        source: Path,
        metadata: FinalMediaMetadata,
    ): PublishedMedia
}
