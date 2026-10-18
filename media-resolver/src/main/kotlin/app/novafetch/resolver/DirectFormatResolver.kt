package app.novafetch.resolver

import app.novafetch.detection.MediaKind
import app.novafetch.detection.DiscoveryMethod
import app.novafetch.download.DownloadPlan
import java.net.URI

object DirectFormatResolver {
    fun toPlan(item: ValidatedCandidate): DownloadPlan.DirectPlan? {
        if (item.candidate.kind == MediaKind.MANIFEST || item.candidate.discoveryMethod == DiscoveryMethod.METADATA) return null
        val name = URI(item.canonicalUrl).path.substringAfterLast('/').ifBlank { "media" }
            .replace(Regex("[^a-zA-Z0-9._-]"), "_").take(120).ifBlank { "media.bin" }
        return DownloadPlan.DirectPlan(item.canonicalUrl, item.candidate.mimeType, name)
    }
}
