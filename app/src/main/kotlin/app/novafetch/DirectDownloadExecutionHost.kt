package app.novafetch

import app.novafetch.common.DownloadId
import app.novafetch.common.DownloadState
import app.novafetch.download.DirectDownloadRunner
import app.novafetch.download.DownloadExecutionHost
import app.novafetch.download.DownloadExecutionOutcome
import app.novafetch.download.DownloadPlan
import app.novafetch.download.DownloadRepository
import app.novafetch.library.MediaFinalizer

class DirectDownloadExecutionHost(
    private val repository: DownloadRepository,
    private val runner: DirectDownloadRunner,
    private val finalizer: MediaFinalizer,
    private val metadataFactory: FinalMediaMetadataFactory,
) : DownloadExecutionHost {
    override suspend fun executeDownload(id: DownloadId): DownloadExecutionOutcome {
        var snapshot = repository.get(id) ?: return DownloadExecutionOutcome.FAILED
        val plan = snapshot.plan as? DownloadPlan.DirectPlan ?: return DownloadExecutionOutcome.FAILED

        snapshot = when (snapshot.state) {
            DownloadState.COMPLETED -> return DownloadExecutionOutcome.COMPLETED
            DownloadState.FAILED,
            DownloadState.CANCELLED,
            DownloadState.PAUSED -> return DownloadExecutionOutcome.FAILED

            DownloadState.VERIFYING,
            DownloadState.FINALIZING -> snapshot

            DownloadState.WAITING_STORAGE,
            DownloadState.CREATED,
            DownloadState.RESOLVING,
            DownloadState.PROCESSING -> return DownloadExecutionOutcome.RETRY

            DownloadState.QUEUED,
            DownloadState.CONNECTING,
            DownloadState.DOWNLOADING,
            DownloadState.WAITING_NETWORK,
            DownloadState.RETRY_PENDING -> runner.run(id)
        }

        if (snapshot.state == DownloadState.FAILED) return DownloadExecutionOutcome.FAILED
        if (snapshot.state != DownloadState.VERIFYING && snapshot.state != DownloadState.FINALIZING) {
            return DownloadExecutionOutcome.RETRY
        }

        val finalized = finalizer.finalize(id, metadataFactory.from(plan))
        return when (finalized.state) {
            DownloadState.COMPLETED -> DownloadExecutionOutcome.COMPLETED
            DownloadState.FAILED,
            DownloadState.CANCELLED -> DownloadExecutionOutcome.FAILED
            else -> DownloadExecutionOutcome.RETRY
        }
    }
}
