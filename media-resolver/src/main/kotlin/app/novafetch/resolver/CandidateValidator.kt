package app.novafetch.resolver

import app.novafetch.detection.*
import app.novafetch.security.UrlPolicy
import java.net.URI

sealed interface ValidationResult {
    data class Accepted(val value: ValidatedCandidate) : ValidationResult
    data class Rejected(val reason: String) : ValidationResult
}

data class ValidatedCandidate(val candidate: MediaCandidate, val canonicalUrl: String, val confidence: Int)

class CandidateValidator {
    fun validate(candidate: MediaCandidate): ValidationResult {
        val page = UrlPolicy.canonicalize(candidate.pageUrl)
            ?: return ValidationResult.Rejected("invalid source page")
        val url = UrlPolicy.canonicalize(candidate.resourceUrl)
            ?: return ValidationResult.Rejected("invalid media URL")
        if (candidate.frameUrl != null && UrlPolicy.canonicalize(candidate.frameUrl) == null)
            return ValidationResult.Rejected("invalid frame URL")
        val mime = candidate.mimeType.orEmpty().substringBefore(';').trim().lowercase()
        val ext = URI(url).path.substringAfterLast('.', "").lowercase()
        val recognized = when (candidate.kind) {
            MediaKind.VIDEO -> mime.startsWith("video/") || ext in setOf("mp4", "m4v", "webm", "mov")
            MediaKind.AUDIO -> mime.startsWith("audio/") || ext in setOf("mp3", "m4a", "aac", "ogg", "opus", "wav", "flac")
            MediaKind.MANIFEST -> ext in setOf("m3u8", "mpd") || mime in setOf("application/vnd.apple.mpegurl", "application/x-mpegurl", "application/dash+xml")
        }
        if (!recognized) return ValidationResult.Rejected("not recognized media")
        val base = when (candidate.discoveryMethod) {
            DiscoveryMethod.DOM -> 85
            DiscoveryMethod.RESOURCE -> 90
            DiscoveryMethod.ADAPTER -> 82
            DiscoveryMethod.DIRECT_LINK -> 75
            DiscoveryMethod.METADATA -> 45
        }
        val sizePenalty = if (candidate.estimatedBytes != null && candidate.estimatedBytes < 128_000) 35 else 0
        return ValidationResult.Accepted(ValidatedCandidate(candidate.copy(pageUrl = page), url, (base - sizePenalty).coerceIn(0, 100)))
    }
}
