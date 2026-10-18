package app.novafetch.library

import app.novafetch.common.DownloadId
import java.nio.file.Path

data class StoredMedia(
    val uri: String,
    val isPending: Boolean,
)

interface SharedMediaStore {
    suspend fun inspect(uri: String): StoredMedia?
    suspend fun insertPending(metadata: FinalMediaMetadata): StoredMedia
    suspend fun write(uri: String, source: Path)
    suspend fun commit(uri: String)
    suspend fun delete(uri: String)
}

class MediaStoreSharedMediaPublisher(
    private val store: SharedMediaStore,
) : SharedMediaPublisher {
    override suspend fun prepare(
        jobId: DownloadId,
        metadata: FinalMediaMetadata,
    ): PendingMedia = store.insertPending(metadata).let { PendingMedia(it.uri) }

    override suspend fun finish(
        pending: PendingMedia,
        source: Path,
    ): PublishedMedia = writeAndCommit(pending.uri, source, deleteOnFailure = true)

    override suspend fun resume(uri: String, source: Path): PublishedMedia? {
        val existing = store.inspect(uri) ?: return null
        if (!existing.isPending) return PublishedMedia(existing.uri)
        return writeAndCommit(existing.uri, source, deleteOnFailure = true)
    }

    private suspend fun writeAndCommit(
        uri: String,
        source: Path,
        deleteOnFailure: Boolean,
    ): PublishedMedia = try {
        store.write(uri, source)
        store.commit(uri)
        PublishedMedia(uri)
    } catch (error: Exception) {
        if (deleteOnFailure) runCatching { store.delete(uri) }
        throw error
    }
}
