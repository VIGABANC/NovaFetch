package app.novafetch

import app.novafetch.download.DownloadPlan
import app.novafetch.library.FinalMediaMetadata

class FinalMediaMetadataFactory {
    fun from(plan: DownloadPlan.DirectPlan): FinalMediaMetadata {
        val mime = plan.expectedMime ?: "application/octet-stream"
        val relativePath = when {
            mime.startsWith("video/") -> "Movies/NovaFetch"
            mime.startsWith("audio/") -> "Music/NovaFetch"
            mime.startsWith("image/") -> "Pictures/NovaFetch"
            else -> "Download/NovaFetch"
        }
        return FinalMediaMetadata(
            displayName = plan.suggestedFileName,
            mimeType = mime,
            relativePath = relativePath,
        )
    }
}
