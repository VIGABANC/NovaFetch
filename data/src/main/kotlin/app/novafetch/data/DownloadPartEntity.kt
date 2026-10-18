package app.novafetch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_parts")
data class DownloadPartEntity(
    @PrimaryKey val id: String,
    val downloadJobId: String,
    val partIndex: Int,
    val type: String,
    val startByte: Long?,
    val endByte: Long?,
    val segmentUrl: String?,
    val expectedBytes: Long?,
    val downloadedBytes: Long,
    val localPath: String,
    val state: String,
    val retryCount: Int,
    val checksum: String?,
)
