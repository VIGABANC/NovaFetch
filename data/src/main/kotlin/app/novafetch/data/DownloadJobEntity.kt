package app.novafetch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_jobs")
data class DownloadJobEntity(
    @PrimaryKey val id: String,
    val sourceUrl: String,
    val pageUrl: String?,
    val title: String,
    val fileName: String,
    val mimeType: String?,
    val mediaType: String,
    val qualityLabel: String?,
    val width: Int?,
    val height: Int?,
    val bitrate: Long?,
    val codec: String?,
    val container: String?,
    val totalBytes: Long?,
    val downloadedBytes: Long,
    val state: String,
    val outputUri: String?,
    val tempPath: String?,
    val supportsResume: Boolean,
    val etag: String?,
    val lastModified: String?,
    val retryCount: Int,
    val failureCode: String?,
    val failureMessage: String?,
    val adapterId: String?,
    val adapterVersion: Int?,
    val createdAtEpochMs: Long,
    val startedAtEpochMs: Long?,
    val updatedAtEpochMs: Long,
    val completedAtEpochMs: Long?,
)
