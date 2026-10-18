package app.novafetch.common

enum class DownloadState {
    CREATED,
    RESOLVING,
    QUEUED,
    CONNECTING,
    DOWNLOADING,
    VERIFYING,
    PROCESSING,
    FINALIZING,
    COMPLETED,
    PAUSED,
    WAITING_NETWORK,
    WAITING_STORAGE,
    RETRY_PENDING,
    FAILED,
    CANCELLED;

    val isTerminal: Boolean
        get() = this == COMPLETED || this == FAILED || this == CANCELLED
}
