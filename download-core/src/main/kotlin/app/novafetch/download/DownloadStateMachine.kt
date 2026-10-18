package app.novafetch.download

import app.novafetch.common.DownloadState

class DownloadStateMachine {
    private val allowed: Map<DownloadState, Set<DownloadState>> = mapOf(
        DownloadState.CREATED to setOf(DownloadState.RESOLVING, DownloadState.CANCELLED),
        DownloadState.RESOLVING to setOf(DownloadState.QUEUED, DownloadState.FAILED, DownloadState.CANCELLED),
        DownloadState.QUEUED to setOf(DownloadState.CONNECTING, DownloadState.PAUSED, DownloadState.CANCELLED),
        DownloadState.CONNECTING to setOf(
            DownloadState.DOWNLOADING,
            DownloadState.WAITING_NETWORK,
            DownloadState.RETRY_PENDING,
            DownloadState.FAILED,
            DownloadState.PAUSED,
            DownloadState.CANCELLED,
        ),
        DownloadState.DOWNLOADING to setOf(
            DownloadState.CONNECTING,
            DownloadState.VERIFYING,
            DownloadState.PAUSED,
            DownloadState.WAITING_NETWORK,
            DownloadState.WAITING_STORAGE,
            DownloadState.RETRY_PENDING,
            DownloadState.FAILED,
            DownloadState.CANCELLED,
        ),
        DownloadState.VERIFYING to setOf(
            DownloadState.PROCESSING,
            DownloadState.FINALIZING,
            DownloadState.FAILED,
            DownloadState.CANCELLED,
        ),
        DownloadState.PROCESSING to setOf(
            DownloadState.FINALIZING,
            DownloadState.FAILED,
            DownloadState.CANCELLED,
        ),
        DownloadState.FINALIZING to setOf(DownloadState.COMPLETED, DownloadState.FAILED),
        DownloadState.PAUSED to setOf(DownloadState.CONNECTING, DownloadState.CANCELLED),
        DownloadState.WAITING_NETWORK to setOf(DownloadState.CONNECTING, DownloadState.PAUSED, DownloadState.CANCELLED),
        DownloadState.WAITING_STORAGE to setOf(DownloadState.QUEUED, DownloadState.PAUSED, DownloadState.CANCELLED),
        DownloadState.RETRY_PENDING to setOf(DownloadState.CONNECTING, DownloadState.PAUSED, DownloadState.FAILED, DownloadState.CANCELLED),
        DownloadState.COMPLETED to emptySet(),
        DownloadState.FAILED to emptySet(),
        DownloadState.CANCELLED to emptySet(),
    )

    fun canTransition(from: DownloadState, to: DownloadState): Boolean =
        allowed[from]?.contains(to) == true
}
