package app.novafetch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val downloadId: String,
    val title: String,
    val mediaType: String,
    val mimeType: String,
    val contentUri: String,
    val fileSize: Long,
    val createdAtEpochMs: Long,
)
