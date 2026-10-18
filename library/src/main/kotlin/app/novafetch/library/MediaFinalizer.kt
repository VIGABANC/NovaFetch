package app.novafetch.library

import app.novafetch.common.DownloadId
import app.novafetch.common.DownloadState
import app.novafetch.download.DownloadRepository
import app.novafetch.download.DownloadSnapshot
import app.novafetch.download.DownloadStateMachine
import java.nio.file.Path

class MediaFinalizer(
    private val repository: DownloadRepository,
    private val verifier: FileIntegrityVerifier,
    private val publisher: SharedMediaPublisher,
    private val libraryRepository: MediaLibraryRepository,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
    private val stateMachine: DownloadStateMachine = DownloadStateMachine(),
) {
    suspend fun finalize(id: DownloadId, metadata: FinalMediaMetadata): DownloadSnapshot {
        var snapshot = requireNotNull(repository.get(id)) { "unknown download: ${id.value}" }
        val path = snapshot.tempPath?.let(Path::of)
            ?: return fail(snapshot, "TEMP_FILE_MISSING")

        if (snapshot.state == DownloadState.FINALIZING) {
            publisher.findPublished(id)?.let { published ->
                upsertLibraryItem(snapshot, path, metadata, published)
                snapshot = transition(snapshot, DownloadState.COMPLETED).copy(
                    outputUri = published.uri,
                    failureCode = null,
                )
                repository.update(snapshot)
                return snapshot
            }
        } else {
            require(snapshot.state == DownloadState.VERIFYING) {
                "MediaFinalizer requires VERIFYING or FINALIZING, got ${snapshot.state}"
            }
            when (val verification = verifier.verify(snapshot, path)) {
                FileVerificationResult.Valid -> Unit
                is FileVerificationResult.Invalid -> return fail(snapshot, verification.code)
            }
            snapshot = transition(snapshot, DownloadState.FINALIZING)
            repository.update(snapshot)
        }

        return try {
            val published = publisher.findPublished(id) ?: publisher.publish(id, path, metadata)
            upsertLibraryItem(snapshot, path, metadata, published)
            snapshot = transition(snapshot, DownloadState.COMPLETED).copy(
                outputUri = published.uri,
                failureCode = null,
            )
            repository.update(snapshot)
            snapshot
        } catch (_: Exception) {
            fail(snapshot, "FINALIZE_FAILED")
        }
    }

    private suspend fun upsertLibraryItem(
        snapshot: DownloadSnapshot,
        path: Path,
        metadata: FinalMediaMetadata,
        published: PublishedMedia,
    ) {
        libraryRepository.upsert(
            MediaItem(
                id = snapshot.id.value,
                downloadId = snapshot.id,
                title = metadata.displayName,
                mediaType = libraryType(metadata.mimeType),
                mimeType = metadata.mimeType,
                contentUri = published.uri,
                fileSize = java.nio.file.Files.size(path),
                createdAtEpochMs = nowEpochMs(),
            ),
        )
    }

    private fun libraryType(mimeType: String): LibraryMediaType = when {
        mimeType.startsWith("video/") -> LibraryMediaType.VIDEO
        mimeType.startsWith("audio/") -> LibraryMediaType.AUDIO
        mimeType.startsWith("image/") -> LibraryMediaType.IMAGE
        else -> LibraryMediaType.FILE
    }

    private suspend fun fail(snapshot: DownloadSnapshot, code: String): DownloadSnapshot {
        if (snapshot.state == DownloadState.FAILED) return snapshot.copy(failureCode = code)
        require(stateMachine.canTransition(snapshot.state, DownloadState.FAILED)) {
            "invalid download transition ${snapshot.state} -> FAILED"
        }
        val failed = snapshot.copy(state = DownloadState.FAILED, failureCode = code)
        repository.update(failed)
        return failed
    }

    private fun transition(snapshot: DownloadSnapshot, target: DownloadState): DownloadSnapshot {
        require(stateMachine.canTransition(snapshot.state, target)) {
            "invalid download transition ${snapshot.state} -> $target"
        }
        return snapshot.copy(state = target)
    }
}
