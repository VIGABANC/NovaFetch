package app.novafetch.download

import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

private fun <T> runSuspend(block: suspend () -> T): T {
    var holder: Result<T>? = null
    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<T>) { holder = result }
    })
    return holder!!.getOrThrow()
}

private fun httpGet(url: String): Int {
    val connection = URI(url).toURL().openConnection() as HttpURLConnection
    return try {
        connection.requestMethod = "GET"
        connection.responseCode
    } finally {
        connection.disconnect()
    }
}

fun main() {
    val base = System.getenv("TEST_MEDIA_BASE_URL") ?: error("TEST_MEDIA_BASE_URL required")
    val fixture = Path.of(System.getenv("TEST_MEDIA_FIXTURE") ?: error("TEST_MEDIA_FIXTURE required"))
    val expected = Files.readAllBytes(fixture)
    val dir = Files.createTempDirectory("novafetch-transfer-test")
    val engine = UrlConnectionTransferEngine(UrlConnectionRangeProbe())

    // Direct full download.
    val direct = dir.resolve("direct.mp4")
    val directResult = runSuspend {
        engine.transfer(TransferRequest("$base/direct.mp4", direct)) { }
    }
    check(directResult is TransferResult.Completed)
    check(!directResult.supportsResume)
    check(Files.readAllBytes(direct).contentEquals(expected))

    // Safe byte-range resume.
    val resume = dir.resolve("resume.mp4")
    val split = 10_000
    Files.write(resume, expected.copyOfRange(0, split))
    val resumeResult = runSuspend {
        engine.transfer(
            TransferRequest(
                url = "$base/range.mp4",
                destination = resume,
                resumeOffset = split.toLong(),
                etag = "\"fixture-v1\"",
            ),
        ) { }
    }
    check(resumeResult is TransferResult.Completed && resumeResult.resumed && !resumeResult.restarted)
    check(resumeResult.supportsResume)
    check(Files.readAllBytes(resume).contentEquals(expected))

    // A malformed 206 response must be rejected before appending any response bytes.
    val malformedRange = dir.resolve("malformed-range.mp4")
    Files.write(malformedRange, expected.copyOfRange(0, split))
    val malformedResult = runCatching {
        runSuspend {
            engine.transfer(
                TransferRequest(
                    url = "$base/wrong-range.mp4",
                    destination = malformedRange,
                    resumeOffset = split.toLong(),
                    etag = "\"fixture-v1\"",
                ),
            ) { }
        }
    }
    check(malformedResult.isFailure)
    check(Files.size(malformedRange) == split.toLong())

    // Server that ignores Range must safely restart, never append duplicate bytes.
    val noRange = dir.resolve("no-range.mp4")
    Files.write(noRange, expected.copyOfRange(0, split))
    val noRangeResult = runSuspend {
        engine.transfer(
            TransferRequest("$base/no-range.mp4", noRange, resumeOffset = split.toLong()),
        ) { }
    }
    check(noRangeResult is TransferResult.Completed && noRangeResult.restarted)
    check(Files.readAllBytes(noRange).contentEquals(expected))

    // ETag change must reject unsafe append and leave partial data untouched.
    check(httpGet("$base/control/etag?value=fixture-v2") == 204)
    val changed = dir.resolve("etag-change.mp4")
    Files.write(changed, expected.copyOfRange(0, split))
    val changedResult = runSuspend {
        engine.transfer(
            TransferRequest(
                url = "$base/range.mp4",
                destination = changed,
                resumeOffset = split.toLong(),
                etag = "\"fixture-v1\"",
            ),
        ) { }
    }
    check(changedResult is TransferResult.SourceChanged)
    check(Files.size(changed) == split.toLong())
    check(httpGet("$base/control/etag?value=fixture-v1") == 204)

    println("UrlConnectionTransferEngineTest: PASS")
}
