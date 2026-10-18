package app.novafetch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaLibraryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MediaItemEntity)

    @Query("SELECT * FROM media_items WHERE downloadId = :downloadId LIMIT 1")
    suspend fun getByDownloadId(downloadId: String): MediaItemEntity?

    @Query("SELECT * FROM media_items ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<MediaItemEntity>>
}
