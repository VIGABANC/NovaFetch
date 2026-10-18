package app.novafetch.common

@JvmInline
value class DownloadId(val value: String) {
    init {
        require(value.isNotBlank()) { "DownloadId cannot be blank" }
    }
}
