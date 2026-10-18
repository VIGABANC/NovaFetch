package app.novafetch.download

sealed interface TransferResult {
    data class Completed(
        val bytesWritten: Long,
        val totalBytes: Long?,
        val supportsResume: Boolean,
        val resumed: Boolean,
        val restarted: Boolean,
        val etag: String?,
        val lastModified: String?,
    ) : TransferResult

    data class SourceChanged(
        val currentEtag: String?,
        val currentLastModified: String?,
    ) : TransferResult
}
