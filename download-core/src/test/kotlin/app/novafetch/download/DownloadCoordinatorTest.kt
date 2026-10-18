package app.novafetch.download

import app.novafetch.common.DownloadId
import app.novafetch.common.DownloadState
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

private fun <T> runSuspendCoordinator(block: suspend () -> T): T {
    var holder: Result<T>? = null
    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<T>) { holder = result }
    })
    return holder!!.getOrThrow()
}

private class FakeRepository(initial: List<DownloadSnapshot> = emptyList()) : DownloadRepository {
    val jobs = initial.associateBy { it.id }.toMutableMap()
    override suspend fun create(snapshot: DownloadSnapshot) { jobs[snapshot.id] = snapshot }
    override suspend fun get(id: DownloadId): DownloadSnapshot? = jobs[id]
    override suspend fun update(snapshot: DownloadSnapshot) { jobs[snapshot.id] = snapshot }
    override suspend fun unfinished(): List<DownloadSnapshot> = jobs.values.filter { !it.state.isTerminal }
    override fun observe(id: DownloadId): Flow<DownloadSnapshot> = object : Flow<DownloadSnapshot> {}
}

private class FakeScheduler : DownloadScheduler {
    val scheduled = mutableListOf<DownloadId>()
    val recoveryScheduled = mutableListOf<DownloadId>()
    val cancelled = mutableListOf<DownloadId>()
    override suspend fun schedule(id: DownloadId) { scheduled += id }
    override suspend fun scheduleRecovery(id: DownloadId) { recoveryScheduled += id }
    override suspend fun cancel(id: DownloadId) { cancelled += id }
}

fun main() {
    val plan = DownloadPlan.DirectPlan("https://example.test/video.mp4", "video/mp4", "video.mp4")

    run {
        val repo = FakeRepository()
        val scheduler = FakeScheduler()
        val coordinator = DownloadCoordinator(repo, scheduler, DownloadStateMachine()) { DownloadId("job-1") }
        val id = runSuspendCoordinator { coordinator.enqueue(plan) }
        check(repo.jobs[id]?.state == DownloadState.QUEUED)
        check(id in scheduler.scheduled)
        runSuspendCoordinator { coordinator.pause(id) }
        check(repo.jobs[id]?.state == DownloadState.PAUSED)
        check(id in scheduler.cancelled)
        runSuspendCoordinator { coordinator.resume(id) }
        check(repo.jobs[id]?.state == DownloadState.CONNECTING)
        check(id in scheduler.scheduled)
        runSuspendCoordinator { coordinator.cancel(id, deletePartial = false) }
        check(repo.jobs[id]?.state == DownloadState.CANCELLED)
        check(id in scheduler.cancelled)
    }

    run {
        val interrupted = DownloadSnapshot(DownloadId("interrupted"), plan, DownloadState.DOWNLOADING)
        val paused = DownloadSnapshot(DownloadId("paused"), plan, DownloadState.PAUSED)
        val repo = FakeRepository(listOf(interrupted, paused))
        val scheduler = FakeScheduler()
        val coordinator = DownloadCoordinator(repo, scheduler, DownloadStateMachine()) { DownloadId("unused") }
        runSuspendCoordinator { coordinator.recoverInterrupted() }
        check(repo.jobs[interrupted.id]?.state == DownloadState.CONNECTING)
        check(interrupted.id in scheduler.recoveryScheduled)
        check(repo.jobs[paused.id]?.state == DownloadState.PAUSED)
        check(paused.id !in scheduler.recoveryScheduled)
    }

    run {
        val verifying = DownloadSnapshot(DownloadId("verifying"), plan, DownloadState.VERIFYING)
        val finalizing = DownloadSnapshot(DownloadId("finalizing"), plan, DownloadState.FINALIZING)
        val repo = FakeRepository(listOf(verifying, finalizing))
        val scheduler = FakeScheduler()
        val coordinator = DownloadCoordinator(repo, scheduler, DownloadStateMachine()) { DownloadId("unused") }
        runSuspendCoordinator { coordinator.recoverInterrupted() }
        check(verifying.id in scheduler.recoveryScheduled)
        check(finalizing.id in scheduler.recoveryScheduled)
        check(repo.jobs[verifying.id]?.state == DownloadState.VERIFYING)
        check(repo.jobs[finalizing.id]?.state == DownloadState.FINALIZING)
    }

    run {
        val running = DownloadSnapshot(DownloadId("network"), plan, DownloadState.DOWNLOADING)
        val repo = FakeRepository(listOf(running))
        val scheduler = FakeScheduler()
        val coordinator = DownloadCoordinator(repo, scheduler, DownloadStateMachine()) { DownloadId("unused") }
        runSuspendCoordinator { coordinator.onNetworkLost(running.id) }
        check(repo.jobs[running.id]?.state == DownloadState.WAITING_NETWORK)
        runSuspendCoordinator { coordinator.onNetworkAvailable(running.id) }
        check(repo.jobs[running.id]?.state == DownloadState.CONNECTING)
        check(running.id in scheduler.scheduled)
    }

    run {
        val retry = RetryPolicy(maxRetries = 3, baseDelayMs = 1_000)
        check(retry.forHttpStatus(503, retryCount = 0) == RetryDecision.RetryAfter(1_000))
        check(retry.forHttpStatus(503, retryCount = 1) == RetryDecision.RetryAfter(2_000))
        check(retry.forHttpStatus(503, retryCount = 2) == RetryDecision.RetryAfter(4_000))
        check(retry.forHttpStatus(503, retryCount = 3) == RetryDecision.Fail)
        check(retry.forHttpStatus(404, retryCount = 0) == RetryDecision.Fail)
    }

    println("DownloadCoordinatorTest: PASS")
}
