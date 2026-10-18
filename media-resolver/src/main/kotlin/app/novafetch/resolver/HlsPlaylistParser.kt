package app.novafetch.resolver

import java.net.URI

/** Bounded, offline HLS playlist inspection. Does not decrypt protected media. */
class HlsPlaylistParser {
    data class Variant(val uri: String, val bandwidth: Long?, val resolution: String?, val codecs: String?)
    data class Playlist(val variants: List<Variant>, val segments: List<String>, val endList: Boolean, val protected: Boolean)

    fun parse(text: String, baseUrl: String): Playlist {
        require(text.length <= 2_000_000) { "Playlist too large" }
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        require(lines.size <= 20_000 && lines.firstOrNull() == "#EXTM3U") { "Invalid HLS playlist" }
        val base = URI(baseUrl)
        require(base.scheme in listOf("http", "https"))
        val variants = mutableListOf<Variant>()
        val segments = mutableListOf<String>()
        var attributes: Map<String, String>? = null
        var protected = false
        var endList = false
        for (line in lines.drop(1)) {
            when {
                line.startsWith("#EXT-X-STREAM-INF:") -> attributes = parseAttributes(line.substringAfter(':'))
                line.startsWith("#EXT-X-KEY:") -> {
                    val key = parseAttributes(line.substringAfter(':'))
                    if (!key["METHOD"].equals("NONE", ignoreCase = true)) protected = true
                }
                line.startsWith("#EXT-X-SESSION-KEY:") -> protected = true
                line == "#EXT-X-ENDLIST" -> endList = true
                line.startsWith("#") -> Unit
                else -> {
                    val uri = base.resolve(line)
                    require(uri.scheme in listOf("http", "https")) { "Unsafe media URL" }
                    val attrs = attributes
                    if (attrs != null) {
                        variants += Variant(uri.toString(), attrs["BANDWIDTH"]?.toLongOrNull(), attrs["RESOLUTION"], attrs["CODECS"])
                        attributes = null
                    } else segments += uri.toString()
                }
            }
        }
        require(attributes == null) { "Variant URI missing" }
        return Playlist(variants, segments, endList, protected)
    }

    private fun parseAttributes(text: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (match in Regex("""([A-Za-z0-9-]+)=("[^"]*"|[^,]*)""").findAll(text)) {
            result[match.groupValues[1]] = match.groupValues[2].trim('"')
        }
        return result
    }
}
