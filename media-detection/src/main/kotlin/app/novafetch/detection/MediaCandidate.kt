package app.novafetch.detection

enum class MediaKind { VIDEO, AUDIO, MANIFEST }
enum class DiscoveryMethod { DOM, DIRECT_LINK, METADATA, RESOURCE, ADAPTER }

data class MediaCandidate(
    val pageUrl: String,
    val frameUrl: String?,
    val resourceUrl: String,
    val kind: MediaKind,
    val mimeType: String? = null,
    val discoveryMethod: DiscoveryMethod,
    val title: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val estimatedBytes: Long? = null,
)
