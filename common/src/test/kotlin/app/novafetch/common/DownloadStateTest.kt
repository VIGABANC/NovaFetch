package app.novafetch.common

fun main() {
    check(DownloadState.COMPLETED.isTerminal) { "COMPLETED must be terminal" }
    check(DownloadState.FAILED.isTerminal) { "FAILED must be terminal" }
    check(DownloadState.CANCELLED.isTerminal) { "CANCELLED must be terminal" }
    check(!DownloadState.PAUSED.isTerminal) { "PAUSED must not be terminal" }
    println("DownloadStateTest: PASS")
}
