package app.novafetch.download

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import app.novafetch.common.DownloadId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkManagerDownloadSchedulerInstrumentedTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @After
    fun tearDown() {
        WorkManager.getInstance(context).cancelAllWork().result.get()
    }

    @Test
    fun scheduleEnqueuesOneTaggedWorkRequest() = runBlocking {
        val id = DownloadId("wm-recovery-test")
        val scheduler = WorkManagerDownloadScheduler(context)

        val result = scheduler.schedule(id, estimatedBytes = null)

        assertEquals(ScheduleResult.Enqueued(SchedulerRoute.WORK_MANAGER), result)
        val work = WorkManager.getInstance(context)
            .getWorkInfosByTag("download:${id.value}")
            .get()
        assertEquals(1, work.size)
        assertTrue(work.single().state in setOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED))
    }
}
