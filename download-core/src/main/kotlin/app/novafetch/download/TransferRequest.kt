package app.novafetch.download

import java.nio.file.Path

data class TransferRequest(
    val url: String,
    val destination: Path,
    val resumeOffset: Long = 0,
    val etag: String? = null,
    val lastModified: String? = null,
    val headers: Map<String, String> = emptyMap(),
) {
    init {
        require(url.isNotBlank()) { "url cannot be blank" }
        require(resumeOffset >= 0) { "resumeOffset cannot be negative" }
    }
}
