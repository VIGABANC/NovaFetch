package app.novafetch.library

import app.novafetch.common.DownloadId

enum class LibraryMediaType {
    VIDEO,
    AUDIO,
    IMAGE,
    FILE,
}

data class MediaItem(
    val id: String,
    val downloadId: DownloadId,
    val title: String,
    val mediaType: LibraryMediaType,
    val mimeType: String,
    val contentUri: String,
    val fileSize: Long,
    val createdAtEpochMs: Long,
)
