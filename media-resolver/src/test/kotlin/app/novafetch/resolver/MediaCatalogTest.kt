package app.novafetch.resolver

import app.novafetch.detection.DiscoveryMethod
import app.novafetch.detection.MediaCandidate
import app.novafetch.detection.MediaKind

private fun candidate(url: String, method: DiscoveryMethod = DiscoveryMethod.DOM, bytes: Long? = 2_000_000): MediaCandidate = MediaCandidate(
    pageUrl = "https://media.example/watch", frameUrl = "https://media.example/watch",
    resourceUrl = url, kind = MediaKind.VIDEO, mimeType = "video/mp4", discoveryMethod = method,
    title = "Example", estimatedBytes = bytes,
)

fun testMediaCatalog() {
    val validator = CandidateValidator()
    val accepted = validator.validate(candidate("https://cdn.example/video.mp4"))
    check(accepted is ValidationResult.Accepted) { "valid video URL should be accepted" }
    val missingFrameAndSize = validator.validate(
        candidate("https://cdn.example/unknown.mp4").copy(frameUrl = null, estimatedBytes = null),
    )
    check(missingFrameAndSize is ValidationResult.Accepted) { "optional frame and size metadata may be absent" }
    val invalidFrame = validator.validate(
        candidate("https://cdn.example/video.mp4").copy(frameUrl = "file:///etc/passwd"),
    )
    check(invalidFrame is ValidationResult.Rejected) { "invalid frame URL should be rejected" }

    val catalog = MediaCatalog()
    catalog.upsert(accepted.value)
    val duplicate = validator.validate(candidate("https://cdn.example/video.mp4#fragment", DiscoveryMethod.METADATA))
    check(duplicate is ValidationResult.Accepted)
    val snapshot = catalog.upsert(duplicate.value)
    check(snapshot.options.size == 1) { "duplicate media should collapse" }
    check(snapshot.options.first().candidate.discoveryMethod == DiscoveryMethod.DOM) { "prefer stronger DOM signal" }

    val blocked = listOf("file:///etc/passwd", "javascript:alert(1)", "blob:https://media.example/x", "https://user:pw@media.example/a.mp4")
    blocked.forEach { check(validator.validate(candidate(it)) is ValidationResult.Rejected) { "must reject $it" } }
    val metadata = candidate("https://cdn.example/path", DiscoveryMethod.METADATA, 10_000)
    check(validator.validate(metadata) is ValidationResult.Accepted)
    val metadataHint = validator.validate(candidate("https://cdn.example/video.mp4", DiscoveryMethod.METADATA))
    check(metadataHint is ValidationResult.Accepted)
    check(DirectFormatResolver.toPlan(metadataHint.value) == null) { "metadata alone must not become a download link" }
    val small = validator.validate(candidate("https://cdn.example/short.mp4", bytes = 30_000))
    check(small is ValidationResult.Accepted)
    check(small.value.confidence < accepted.value.confidence) { "tiny files should be deprioritized, not rejected" }
    val manifest = validator.validate(candidate("https://cdn.example/master.m3u8").copy(kind=MediaKind.MANIFEST, mimeType="application/vnd.apple.mpegurl"))
    check(manifest is ValidationResult.Accepted)
    check(DirectFormatResolver.toPlan(manifest.value) == null) { "HLS must not be handed to direct transfer" }
    check(DirectFormatResolver.toPlan(accepted.value)?.sourceUrl == "https://cdn.example/video.mp4")
    println("MediaCatalogTest: PASS")
}

fun main() = testMediaCatalog()
