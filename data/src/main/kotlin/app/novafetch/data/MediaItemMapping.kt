package app.novafetch.data

import app.novafetch.common.DownloadId
import app.novafetch.library.LibraryMediaType
import app.novafetch.library.MediaItem

fun MediaItem.toEntity(): MediaItemEntity = MediaItemEntity(
    id = id,
    downloadId = downloadId.value,
    title = title,
    mediaType = mediaType.name,
    mimeType = mimeType,
    contentUri = contentUri,
    fileSize = fileSize,
    createdAtEpochMs = createdAtEpochMs,
)

fun MediaItemEntity.toMediaItem(): MediaItem = MediaItem(
    id = id,
    downloadId = DownloadId(downloadId),
    title = title,
    mediaType = LibraryMediaType.valueOf(mediaType),
    mimeType = mimeType,
    contentUri = contentUri,
    fileSize = fileSize,
    createdAtEpochMs = createdAtEpochMs,
)
