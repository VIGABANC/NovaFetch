package app.novafetch.download

sealed interface DownloadPlan {
    data class DirectPlan(
        val sourceUrl: String,
        val expectedMime: String?,
        val suggestedFileName: String,
    ) : DownloadPlan {
        init {
            require(sourceUrl.isNotBlank()) { "sourceUrl cannot be blank" }
            require(suggestedFileName.isNotBlank()) { "suggestedFileName cannot be blank" }
        }
    }
}
