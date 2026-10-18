package app.novafetch.download

data class RangeCapability(
    val supportsRanges: Boolean,
    val contentLength: Long?,
    val etag: String?,
    val lastModified: String?,
)

interface RangeProbe {
    suspend fun probe(url: String, headers: Map<String, String> = emptyMap()): RangeCapability
}
