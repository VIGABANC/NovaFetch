package app.novafetch.download

import app.novafetch.common.DownloadId
import app.novafetch.common.DownloadState
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CancellationException

class DirectDownloadRunner(
    private val repository: DownloadRepository,
    private val transferEngine: TransferEngine,
    private val tempPathProvider: DownloadTempPathProvider,
    private val stateMachine: DownloadStateMachine = DownloadStateMachine(),
) {
    suspend fun run(id: DownloadId): DownloadSnapshot {
        var snapshot = requireNotNull(repository.get(id)) { "unknown download: ${id.value}" }
        val plan = snapshot.plan as? DownloadPlan.DirectPlan
            ?: error("DirectDownloadRunner only supports DirectPlan")
        val destination = snapshot.tempPath?.let(Path::of)
            ?: tempPathProvider.pathFor(id, plan.suggestedFileName)

        snapshot = (if (snapshot.state == DownloadState.CONNECTING) {
            snapshot
        } else {
            transition(snapshot, DownloadState.CONNECTING)
        }).copy(
            tempPath = destination.toString(),
        )
        repository.update(snapshot)

        snapshot = transition(snapshot, DownloadState.DOWNLOADING)
        repository.update(snapshot)

        val resumeOffset = safeResumeOffset(snapshot, destination)
        val result = transferEngine.transfer(
            TransferRequest(
                url = plan.sourceUrl,
                destination = destination,
                resumeOffset = resumeOffset,
                etag = snapshot.etag,
                lastModified = snapshot.lastModified,
            ),
        ) { downloadedBytes ->
            val persisted = requireNotNull(repository.get(id)) { "download disappeared: ${id.value}" }
            if (persisted.state == DownloadState.PAUSED) {
                snapshot = persisted.copy(
                    downloadedBytes = downloadedBytes,
                    tempPath = destination.toString(),
                )
                repository.update(snapshot)
            }
            if (persisted.state != DownloadState.DOWNLOADING && persisted.state != DownloadState.PAUSED) {
                throw CancellationException("download execution is no longer active")
            }
            if (persisted.state == DownloadState.PAUSED) {
                throw CancellationException("download paused")
            }
            snapshot = persisted.copy(
                downloadedBytes = downloadedBytes,
                tempPath = destination.toString(),
            )
            repository.update(snapshot)
        }

        val latest = requireNotNull(repository.get(id)) { "download disappeared: ${id.value}" }
        if (latest.state != DownloadState.DOWNLOADING) return latest
        snapshot = latest

        snapshot = when (result) {
            is TransferResult.Completed -> transition(snapshot, DownloadState.VERIFYING).copy(
                downloadedBytes = Files.size(destination),
                totalBytes = result.totalBytes,
                tempPath = destination.toString(),
                etag = result.etag,
                lastModified = result.lastModified,
                supportsResume = result.supportsResume,
                failureCode = null,
            )

            is TransferResult.SourceChanged -> transition(snapshot, DownloadState.FAILED).copy(
                etag = result.currentEtag,
                lastModified = result.currentLastModified,
                failureCode = "SOURCE_CHANGED",
            )
        }
        repository.update(snapshot)
        return snapshot
    }

    private fun safeResumeOffset(snapshot: DownloadSnapshot, destination: Path): Long {
        if (snapshot.downloadedBytes <= 0L) return 0L
        if (!Files.exists(destination)) return 0L
        return if (Files.size(destination) == snapshot.downloadedBytes) snapshot.downloadedBytes else 0L
    }

    private fun transition(snapshot: DownloadSnapshot, target: DownloadState): DownloadSnapshot {
        require(stateMachine.canTransition(snapshot.state, target)) {
            "invalid download transition ${snapshot.state} -> $target"
        }
        return snapshot.copy(state = target)
    }
}
