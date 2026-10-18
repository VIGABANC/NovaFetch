package app.novafetch.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.novafetch.common.DownloadId
import app.novafetch.common.DownloadState
import app.novafetch.download.DownloadCoordinator
import app.novafetch.download.DownloadPlan
import app.novafetch.download.DownloadScheduler
import app.novafetch.download.DownloadSnapshot
import app.novafetch.download.DownloadStateMachine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class NovaFetchDatabaseInstrumentedTest {
    private lateinit var database: NovaFetchDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NovaFetchDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun downloadAndLibraryRowsRoundTripThroughRealRoom() = runBlocking {
        val job = DownloadJobEntity(
            id = "job-1",
            sourceUrl = "https://example.test/video.mp4",
            pageUrl = null,
            title = "video.mp4",
            fileName = "video.mp4",
            mimeType = "video/mp4",
            mediaType = "VIDEO",
            qualityLabel = "1080p",
            width = 1920,
            height = 1080,
            bitrate = null,
            codec = "avc1",
            container = "mp4",
            totalBytes = 1024L,
            downloadedBytes = 512L,
            state = "DOWNLOADING",
            outputUri = null,
            tempPath = "/tmp/video.part",
            supportsResume = true,
            etag = "etag-1",
            lastModified = null,
            retryCount = 0,
            failureCode = null,
            failureMessage = null,
            adapterId = null,
            adapterVersion = null,
            createdAtEpochMs = 1L,
            startedAtEpochMs = 2L,
            updatedAtEpochMs = 3L,
            completedAtEpochMs = null,
        )
        database.downloadDao().upsertJob(job)
        assertEquals(job, database.downloadDao().getJob(job.id))

        val media = MediaItemEntity(
            id = "media-1",
            downloadId = job.id,
            title = "video.mp4",
            mediaType = "VIDEO",
            mimeType = "video/mp4",
            contentUri = "content://media/video/1",
            fileSize = 1024L,
            createdAtEpochMs = 4L,
        )
        database.mediaLibraryDao().upsert(media)
        val restored = database.mediaLibraryDao().getByDownloadId(job.id)
        assertNotNull(restored)
        assertEquals(media, restored)
    }

    @Test
    fun interruptedDownloadRecoversAfterDatabaseReopen() = runBlocking {
        database.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "process-recovery-${System.nanoTime()}.db"
        val partial = File(context.cacheDir, "process-recovery.part").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        val id = DownloadId("process-recovery-${System.nanoTime()}")
        val expected = DownloadSnapshot(
            id = id,
            plan = DownloadPlan.DirectPlan(
                "https://example.test/interrupted.mp4",
                "video/mp4",
                "interrupted.mp4",
            ),
            state = DownloadState.DOWNLOADING,
            downloadedBytes = partial.length(),
            totalBytes = 100L,
            tempPath = partial.absolutePath,
            etag = "\"stable-etag\"",
            supportsResume = true,
        )
        val firstOpen = Room.databaseBuilder(context, NovaFetchDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        val firstRepository = RoomDownloadRepository(firstOpen.downloadDao())
        firstRepository.create(expected)
        firstRepository.update(expected)
        firstOpen.close()

        val reopened = Room.databaseBuilder(context, NovaFetchDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = RoomDownloadRepository(reopened.downloadDao())
            val restored = requireNotNull(repository.get(id))
            assertEquals(DownloadState.DOWNLOADING, restored.state)
            assertEquals(expected.downloadedBytes, restored.downloadedBytes)
            assertEquals(expected.tempPath, restored.tempPath)
            assertEquals(expected.etag, restored.etag)

            val scheduler = RecoveryRecordingScheduler()
            DownloadCoordinator(repository, scheduler, DownloadStateMachine()).recoverInterrupted()
            val recovered = requireNotNull(repository.get(id))
            assertEquals(DownloadState.CONNECTING, recovered.state)
            assertEquals(expected.downloadedBytes, recovered.downloadedBytes)
            assertTrue(id in scheduler.recoveryIds)
        } finally {
            reopened.close()
            context.deleteDatabase(databaseName)
            partial.delete()
        }
    }
}

private class RecoveryRecordingScheduler : DownloadScheduler {
    val recoveryIds = mutableListOf<DownloadId>()
    override suspend fun schedule(id: DownloadId) = Unit
    override suspend fun scheduleRecovery(id: DownloadId) { recoveryIds += id }
    override suspend fun cancel(id: DownloadId) = Unit
}
