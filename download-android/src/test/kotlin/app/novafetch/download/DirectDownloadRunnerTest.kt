package app.novafetch.download

import app.novafetch.common.DownloadId
import app.novafetch.common.DownloadState
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlinx.coroutines.flow.Flow

private fun <T> runSuspendRunner(block: suspend () -> T): T {
    var holder: Result<T>? = null
    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<T>) { holder = result }
    })
    return holder!!.getOrThrow()
}

private class RunnerRepository(initial: DownloadSnapshot) : DownloadRepository {
    var current = initial
    val history = mutableListOf(initial)

    override suspend fun create(snapshot: DownloadSnapshot) {
        current = snapshot
        history += snapshot
    }

    override suspend fun get(id: DownloadId): DownloadSnapshot? =
        current.takeIf { it.id == id }

    override suspend fun update(snapshot: DownloadSnapshot) {
        current = snapshot
        history += snapshot
    }

    override suspend fun unfinished(): List<DownloadSnapshot> = listOf(current)
    override fun observe(id: DownloadId): Flow<DownloadSnapshot> = object : Flow<DownloadSnapshot> {}
}

fun main() {
    val base = System.getenv("TEST_MEDIA_BASE_URL") ?: error("TEST_MEDIA_BASE_URL required")
    val fixture = Path.of(System.getenv("TEST_MEDIA_FIXTURE") ?: error("TEST_MEDIA_FIXTURE required"))
    val expected = Files.readAllBytes(fixture)
    val tempDir = Files.createTempDirectory("novafetch-runner-test")
    val id = DownloadId("runner-job")
    val repo = RunnerRepository(
        DownloadSnapshot(
            id = id,
            plan = DownloadPlan.DirectPlan(
                sourceUrl = "$base/range.mp4",
                expectedMime = "video/mp4",
                suggestedFileName = "fixture.mp4",
            ),
            state = DownloadState.QUEUED,
        ),
    )
    val engine = UrlConnectionTransferEngine(UrlConnectionRangeProbe())
    val runner = DirectDownloadRunner(
        repository = repo,
        transferEngine = engine,
        tempPathProvider = DownloadTempPathProvider { _, fileName ->
            tempDir.resolve("$fileName.part")
        },
    )

    val result = runSuspendRunner { runner.run(id) }

    check(result.state == DownloadState.VERIFYING)
    check(result.downloadedBytes == expected.size.toLong())
    check(result.totalBytes == expected.size.toLong())
    check(result.tempPath != null)
    check(result.supportsResume)
    check(result.etag == "\"fixture-v1\"")
    check(Files.readAllBytes(Path.of(result.tempPath)).contentEquals(expected))
    check(repo.history.any { it.state == DownloadState.CONNECTING })
    check(repo.history.any { it.state == DownloadState.DOWNLOADING })
    check(repo.history.last().state == DownloadState.VERIFYING)

    // Resume path: coordinator/recovery schedules jobs in CONNECTING state.
    val resumeId = DownloadId("runner-resume-job")
    val partialPath = tempDir.resolve("resume-fixture.mp4.part")
    val split = 10_000
    Files.write(partialPath, expected.copyOfRange(0, split))
    val resumeRepo = RunnerRepository(
        DownloadSnapshot(
            id = resumeId,
            plan = DownloadPlan.DirectPlan(
                sourceUrl = "$base/range.mp4",
                expectedMime = "video/mp4",
                suggestedFileName = "resume-fixture.mp4",
            ),
            state = DownloadState.CONNECTING,
            downloadedBytes = split.toLong(),
            totalBytes = expected.size.toLong(),
            tempPath = partialPath.toString(),
            etag = "\"fixture-v1\"",
            supportsResume = true,
        ),
    )
    val resumeRunner = DirectDownloadRunner(
        repository = resumeRepo,
        transferEngine = engine,
        tempPathProvider = DownloadTempPathProvider { _, fileName ->
            tempDir.resolve("$fileName.part")
        },
    )

    val resumed = runSuspendRunner { resumeRunner.run(resumeId) }
    check(resumed.state == DownloadState.VERIFYING)
    check(resumed.downloadedBytes == expected.size.toLong())
    check(Files.readAllBytes(partialPath).contentEquals(expected))
    check(resumeRepo.history.any { it.state == DownloadState.DOWNLOADING && it.downloadedBytes == split.toLong() })

    println("DirectDownloadRunnerTest: PASS")
}
