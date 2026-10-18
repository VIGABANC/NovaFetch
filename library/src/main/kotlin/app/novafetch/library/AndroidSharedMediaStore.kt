package app.novafetch.library

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import java.nio.file.Files
import java.nio.file.Path

class AndroidSharedMediaStore(
    private val resolver: ContentResolver,
) : SharedMediaStore {
    override suspend fun inspect(uri: String): StoredMedia? {
        val parsed = Uri.parse(uri)
        val projection = arrayOf(MediaStore.MediaColumns.IS_PENDING)
        resolver.query(parsed, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val pending = cursor.getInt(
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_PENDING),
            ) != 0
            return StoredMedia(uri = uri, isPending = pending)
        }
        return null
    }

    override suspend fun insertPending(metadata: FinalMediaMetadata): StoredMedia {
        val collection = collectionFor(metadata.mimeType)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, metadata.displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, metadata.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, normalizeRelativePath(metadata.relativePath))
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(collection, values)) {
            "MediaStore insert returned null"
        }
        return StoredMedia(uri.toString(), isPending = true)
    }

    override suspend fun write(uri: String, source: Path) {
        val destination = requireNotNull(resolver.openOutputStream(Uri.parse(uri), "w")) {
            "MediaStore output stream unavailable: $uri"
        }
        destination.use { output ->
            Files.newInputStream(source).use { input ->
                input.copyTo(output)
            }
        }
    }

    override suspend fun commit(uri: String) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        val updated = resolver.update(Uri.parse(uri), values, null, null)
        require(updated > 0) { "MediaStore publish failed: $uri" }
    }

    override suspend fun delete(uri: String) {
        resolver.delete(Uri.parse(uri), null, null)
    }

    private fun collectionFor(mimeType: String): Uri = when {
        mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        mimeType.startsWith("audio/") -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        mimeType.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
    }

    private fun normalizeRelativePath(path: String): String =
        path.trim().trim('/').let { "$it/" }
}
