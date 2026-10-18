package app.novafetch.common

fun testDownloadId() {
    check(DownloadId("abc").value == "abc")
    val error = runCatching { DownloadId("   ") }.exceptionOrNull()
    check(error is IllegalArgumentException) { "blank DownloadId must be rejected" }
}
