package app.novafetch.download

import app.novafetch.common.DownloadState

fun main() {
    val sm = DownloadStateMachine()
    check(sm.canTransition(DownloadState.DOWNLOADING, DownloadState.PAUSED))
    check(sm.canTransition(DownloadState.DOWNLOADING, DownloadState.CONNECTING)) { "recovery reconnect must be legal" }
    check(sm.canTransition(DownloadState.PAUSED, DownloadState.CONNECTING))
    check(sm.canTransition(DownloadState.DOWNLOADING, DownloadState.WAITING_NETWORK))
    check(sm.canTransition(DownloadState.WAITING_NETWORK, DownloadState.CONNECTING))
    check(!sm.canTransition(DownloadState.COMPLETED, DownloadState.DOWNLOADING))
    check(!sm.canTransition(DownloadState.FAILED, DownloadState.DOWNLOADING))
    check(!sm.canTransition(DownloadState.CANCELLED, DownloadState.CONNECTING))
    println("DownloadStateMachineTest: PASS")
}
