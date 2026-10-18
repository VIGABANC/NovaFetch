package app.novafetch.download

interface TransferEngine {
    suspend fun transfer(
        request: TransferRequest,
        onProgress: suspend (downloadedBytes: Long) -> Unit,
    ): TransferResult
}
