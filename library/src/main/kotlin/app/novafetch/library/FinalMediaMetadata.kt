package app.novafetch.library

data class FinalMediaMetadata(
    val displayName: String,
    val mimeType: String,
    val relativePath: String,
) {
    init {
        require(displayName.isNotBlank())
        require(mimeType.isNotBlank())
        require(relativePath.isNotBlank())
    }
}
