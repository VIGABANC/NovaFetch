package app.novafetch.download

import app.novafetch.common.DownloadId
import app.novafetch.common.DownloadState
import java.util.UUID

class DownloadCoordinator(
    private val repository: DownloadRepository,
    private val scheduler: DownloadScheduler,
    private val stateMachine: DownloadStateMachine,
    private val idFactory: () -> DownloadId = { DownloadId(UUID.randomUUID().toString()) },
) {
    suspend fun enqueue(plan: DownloadPlan): DownloadId {
        val id = idFactory()
        repository.create(DownloadSnapshot(id = id, plan = plan, state = DownloadState.QUEUED))
        scheduler.schedule(id)
        return id
    }

    suspend fun pause(id: DownloadId) {
        val current = requireJob(id)
        transition(current, DownloadState.PAUSED)
        scheduler.cancel(id)
    }

    suspend fun resume(id: DownloadId) {
        val current = requireJob(id)
        transition(current, DownloadState.CONNECTING)
        scheduler.schedule(id)
    }

    suspend fun cancel(id: DownloadId, deletePartial: Boolean) {
        val current = requireJob(id)
        if (!current.state.isTerminal) {
            transition(current, DownloadState.CANCELLED)
            scheduler.cancel(id)
        }
        // Partial-file deletion belongs to the Android storage adapter, not the core coordinator.
        @Suppress("UNUSED_VARIABLE")
        val requestedDelete = deletePartial
    }

    suspend fun onNetworkLost(id: DownloadId) {
        val current = requireJob(id)
        transition(current, DownloadState.WAITING_NETWORK)
    }

    suspend fun onNetworkAvailable(id: DownloadId) {
        val current = requireJob(id)
        transition(current, DownloadState.CONNECTING)
        scheduler.schedule(id)
    }

    suspend fun recoverInterrupted() {
        repository.unfinished().forEach { current ->
            when (current.state) {
                DownloadState.PAUSED,
                DownloadState.WAITING_STORAGE,
                DownloadState.CREATED,
                DownloadState.RESOLVING,
                DownloadState.VERIFYING,
                DownloadState.PROCESSING,
                DownloadState.FINALIZING -> Unit

                DownloadState.QUEUED,
                DownloadState.CONNECTING -> scheduler.schedule(current.id)

                DownloadState.DOWNLOADING,
                DownloadState.WAITING_NETWORK,
                DownloadState.RETRY_PENDING -> {
                    transition(current, DownloadState.CONNECTING)
                    scheduler.schedule(current.id)
                }

                DownloadState.COMPLETED,
                DownloadState.FAILED,
                DownloadState.CANCELLED -> Unit
            }
        }
    }

    private suspend fun requireJob(id: DownloadId): DownloadSnapshot =
        requireNotNull(repository.get(id)) { "unknown download: ${id.value}" }

    private suspend fun transition(current: DownloadSnapshot, target: DownloadState) {
        require(stateMachine.canTransition(current.state, target)) {
            "illegal transition ${current.state} -> $target"
        }
        repository.update(current.copy(state = target))
    }
}
