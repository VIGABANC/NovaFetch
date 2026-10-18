package app.novafetch.download

fun main() {
    val valid = DownloadPlan.DirectPlan(
        sourceUrl = "https://example.test/video.mp4",
        expectedMime = "video/mp4",
        suggestedFileName = "video.mp4",
    )
    check(valid.sourceUrl.startsWith("https://"))
    check(runCatching {
        DownloadPlan.DirectPlan("", null, "video.mp4")
    }.exceptionOrNull() is IllegalArgumentException)
    check(runCatching {
        DownloadPlan.DirectPlan("https://example.test/video.mp4", null, " ")
    }.exceptionOrNull() is IllegalArgumentException)
    println("DownloadPlanTest: PASS")
}
