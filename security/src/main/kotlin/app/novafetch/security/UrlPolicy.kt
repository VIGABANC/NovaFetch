package app.novafetch.security

import java.net.URI

object UrlPolicy {
    fun allowRemoteMedia(raw: String): Boolean = canonicalize(raw) != null
    fun canonicalize(raw: String): String? {
        if (raw.length !in 1..8192 || raw.any { it.isISOControl() }) return null
        return try {
            val uri = URI(raw).normalize()
            if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank() || uri.rawUserInfo != null) null
            else uri.toASCIIString().substringBefore('#')
        } catch (_: Exception) { null }
    }
}
