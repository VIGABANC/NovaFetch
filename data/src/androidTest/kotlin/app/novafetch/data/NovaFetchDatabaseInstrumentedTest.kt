package app.novafetch.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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
}
