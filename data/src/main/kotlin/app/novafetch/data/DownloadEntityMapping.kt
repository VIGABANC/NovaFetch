package app.novafetch.data

import app.novafetch.common.DownloadId
import app.novafetch.common.DownloadState
import app.novafetch.download.DownloadPlan
import app.novafetch.download.DownloadSnapshot

fun DownloadSnapshot.toEntity(
    title: String,
    tempPath: String?,
    createdAtEpochMs: Long,
    updatedAtEpochMs: Long,
): DownloadJobEntity {
    val direct = plan as? DownloadPlan.DirectPlan
        ?: error("Plan type not supported by schema v1: ${plan::class.simpleName}")
    return DownloadJobEntity(
        id = id.value,
        sourceUrl = direct.sourceUrl,
        pageUrl = null,
        title = title,
        fileName = direct.suggestedFileName,
        mimeType = direct.expectedMime,
        mediaType = mediaTypeFor(direct.expectedMime),
        qualityLabel = null,
        width = null,
        height = null,
        bitrate = null,
        codec = null,
        container = direct.expectedMime?.substringAfter('/', missingDelimiterValue = "")?.ifBlank { null },
        totalBytes = totalBytes,
        downloadedBytes = downloadedBytes,
        state = state.name,
        outputUri = outputUri,
        tempPath = tempPath ?: this.tempPath,
        supportsResume = supportsResume,
        etag = etag,
        lastModified = lastModified,
        retryCount = retryCount,
        failureCode = failureCode,
        failureMessage = null,
        adapterId = null,
        adapterVersion = null,
        createdAtEpochMs = createdAtEpochMs,
        startedAtEpochMs = null,
        updatedAtEpochMs = updatedAtEpochMs,
        completedAtEpochMs = null,
    )
}

fun DownloadJobEntity.toSnapshot(): DownloadSnapshot = DownloadSnapshot(
    id = DownloadId(id),
    plan = DownloadPlan.DirectPlan(
        sourceUrl = sourceUrl,
        expectedMime = mimeType,
        suggestedFileName = fileName,
    ),
    state = DownloadState.valueOf(state),
    downloadedBytes = downloadedBytes,
    totalBytes = totalBytes,
    tempPath = tempPath,
    etag = etag,
    lastModified = lastModified,
    supportsResume = supportsResume,
    outputUri = outputUri,
    retryCount = retryCount,
    failureCode = failureCode,
)

private fun mediaTypeFor(mimeType: String?): String = when {
    mimeType?.startsWith("video/") == true -> "VIDEO"
    mimeType?.startsWith("audio/") == true -> "AUDIO"
    mimeType?.startsWith("image/") == true -> "IMAGE"
    else -> "FILE"
}
