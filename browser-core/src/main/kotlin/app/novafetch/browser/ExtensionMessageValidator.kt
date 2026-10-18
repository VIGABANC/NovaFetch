package app.novafetch.browser

import app.novafetch.detection.DiscoveryMethod
import app.novafetch.detection.MediaCandidate
import app.novafetch.detection.MediaKind
import app.novafetch.security.UrlPolicy

/** Sender properties must be obtained from GeckoView, not from page-controlled JSON. */
data class ExtensionSenderContext(
    val extensionId: String,
    val sessionMatches: Boolean,
    val frameUrl: String,
)
data class DomMediaHint(val url: String, val kind: String, val method: String, val mimeType: String?)
data class DomMediaMessage(
    val schemaVersion: Int,
    val type: String,
    val pageUrl: String,
    val items: List<DomMediaHint>,
)

class ExtensionMessageValidator {
    fun validate(sender: ExtensionSenderContext, message: DomMediaMessage): Result<List<MediaCandidate>> = runCatching {
        require(sender.extensionId == EXTENSION_ID && sender.sessionMatches) { "untrusted extension sender" }
        require(message.schemaVersion == 1 && message.type == "media_candidates") { "unsupported payload" }
        require(message.items.size in 1..10 && message.pageUrl.length <= 2048) { "invalid batch size" }
        val page = requireNotNull(UrlPolicy.canonicalize(message.pageUrl)) { "invalid page URL" }
        val frame = requireNotNull(UrlPolicy.canonicalize(sender.frameUrl)) { "invalid frame URL" }
        require(page == frame) { "page origin does not match actual sender frame" }
        message.items.map { item ->
            require(item.url.length <= 2048 && (item.mimeType?.length ?: 0) <= 100) { "oversized hint" }
            val kind = when (item.kind) {
                "video" -> MediaKind.VIDEO
                "audio" -> MediaKind.AUDIO
                "manifest" -> MediaKind.MANIFEST
                else -> error("unsupported media type")
            }
            val method = when (item.method) {
                "dom" -> DiscoveryMethod.DOM
                "metadata" -> DiscoveryMethod.METADATA
                "direct_link" -> DiscoveryMethod.DIRECT_LINK
                else -> error("unsupported discovery method")
            }
            require(UrlPolicy.allowRemoteMedia(item.url)) { "invalid resource" }
            MediaCandidate(page, frame, item.url, kind, item.mimeType, method)
        }
    }
    companion object { const val EXTENSION_ID = "media-detector@novafetch.app" }
}
