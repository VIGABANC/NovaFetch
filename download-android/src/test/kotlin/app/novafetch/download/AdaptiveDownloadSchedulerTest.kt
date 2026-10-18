package app.novafetch.download

import app.novafetch.common.DownloadId
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

private fun <T> runSuspendScheduler(block: suspend () -> T): T {
    var holder: Result<T>? = null
    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<T>) { holder = result }
    })
    return holder!!.getOrThrow()
}

private class FakePlatformScheduler(private val route: SchedulerRoute) : PlatformDownloadScheduler {
    val scheduled = mutableListOf<Pair<DownloadId, Long?>>()
    val cancelled = mutableListOf<DownloadId>()
    override suspend fun schedule(id: DownloadId, estimatedBytes: Long?): ScheduleResult {
        scheduled += id to estimatedBytes
        return ScheduleResult.Enqueued(route)
    }
    override suspend fun cancel(id: DownloadId) { cancelled += id }
}

fun main() {
    val id = DownloadId("job-34")
    val uidt = FakePlatformScheduler(SchedulerRoute.UIDT)
    val work = FakePlatformScheduler(SchedulerRoute.WORK_MANAGER)
    val api34 = AdaptiveDownloadScheduler({ 34 }, DownloadSchedulerPolicy(), uidt, work)
    check(runSuspendScheduler { api34.scheduleWithEstimate(id, 1_000L) } == ScheduleResult.Enqueued(SchedulerRoute.UIDT))
    check(uidt.scheduled == listOf(id to 1_000L))
    check(work.scheduled.isEmpty())

    val api33 = AdaptiveDownloadScheduler({ 33 }, DownloadSchedulerPolicy(), uidt, work)
    val id33 = DownloadId("job-33")
    check(runSuspendScheduler { api33.scheduleWithEstimate(id33, 2_000L) } == ScheduleResult.Enqueued(SchedulerRoute.WORK_MANAGER))
    check(work.scheduled.contains(id33 to 2_000L))

    val recoveryId = DownloadId("recovery-34")
    runSuspendScheduler { api34.scheduleRecovery(recoveryId) }
    check(work.scheduled.contains(recoveryId to null))
    check(uidt.scheduled.none { it.first == recoveryId })

    runSuspendScheduler { api34.cancel(id) }
    check(id in uidt.cancelled)
    check(id in work.cancelled)

    println("AdaptiveDownloadSchedulerTest: PASS")
}
