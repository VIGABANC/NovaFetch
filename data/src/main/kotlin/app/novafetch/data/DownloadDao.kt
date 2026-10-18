package app.novafetch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJob(job: DownloadJobEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPart(part: DownloadPartEntity)

    @Query("SELECT * FROM download_jobs WHERE id = :id LIMIT 1")
    suspend fun getJob(id: String): DownloadJobEntity?

    @Query("SELECT * FROM download_jobs WHERE id = :id LIMIT 1")
    fun observeJob(id: String): Flow<DownloadJobEntity?>

    @Query("SELECT * FROM download_jobs WHERE state NOT IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    suspend fun unfinishedJobs(): List<DownloadJobEntity>

    @Query("UPDATE download_jobs SET downloadedBytes = :downloadedBytes, state = :state, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun updateProgress(id: String, downloadedBytes: Long, state: String, updatedAtEpochMs: Long)

    @Query("SELECT * FROM download_parts WHERE downloadJobId = :downloadJobId ORDER BY partIndex")
    suspend fun partsForJob(downloadJobId: String): List<DownloadPartEntity>
}
