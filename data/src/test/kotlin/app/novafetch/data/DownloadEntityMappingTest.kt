package app.novafetch.data

import app.novafetch.common.DownloadId
import app.novafetch.common.DownloadState
import app.novafetch.download.DownloadPlan
import app.novafetch.download.DownloadSnapshot

fun main() {
    val snapshot = DownloadSnapshot(
        id = DownloadId("job-42"),
        plan = DownloadPlan.DirectPlan(
            sourceUrl = "https://example.test/video.mp4",
            expectedMime = "video/mp4",
            suggestedFileName = "video.mp4",
        ),
        state = DownloadState.PAUSED,
        downloadedBytes = 12_345,
        retryCount = 2,
        failureCode = null,
        totalBytes = 99_999,
        tempPath = "/private/job-42.part",
        etag = "\"etag-v1\"",
        lastModified = "Sat, 17 Oct 2026 00:00:00 GMT",
        supportsResume = true,
        outputUri = "content://media/external/video/42",
    )

    val entity = snapshot.toEntity(
        title = "Example video",
        tempPath = "/private/job-42.part",
        createdAtEpochMs = 1000,
        updatedAtEpochMs = 2000,
    )
    check(entity.id == "job-42")
    check(entity.state == "PAUSED")
    check(entity.sourceUrl == "https://example.test/video.mp4")
    check(entity.downloadedBytes == 12_345L)
    check(entity.fileName == "video.mp4")
    check(entity.mediaType == "VIDEO")

    val roundTrip = entity.toSnapshot()
    check(roundTrip.id == snapshot.id)
    check(roundTrip.state == snapshot.state)
    check(roundTrip.plan == snapshot.plan)
    check(roundTrip.downloadedBytes == snapshot.downloadedBytes)
    check(roundTrip.retryCount == snapshot.retryCount)
    check(roundTrip.totalBytes == snapshot.totalBytes)
    check(roundTrip.tempPath == snapshot.tempPath)
    check(roundTrip.etag == snapshot.etag)
    check(roundTrip.lastModified == snapshot.lastModified)
    check(roundTrip.supportsResume == snapshot.supportsResume)
    check(roundTrip.outputUri == snapshot.outputUri)

    println("DownloadEntityMappingTest: PASS")
}
