package app.novafetch.download

import java.net.HttpURLConnection
import java.net.URI

class UrlConnectionRangeProbe : RangeProbe {
    override suspend fun probe(url: String, headers: Map<String, String>): RangeCapability {
        val connection = open(url, headers)
        return try {
            connection.setRequestProperty("Range", "bytes=0-0")
            val status = connection.responseCode
            val etag = connection.getHeaderField("ETag")
            val lastModified = connection.getHeaderField("Last-Modified")
            when (status) {
                HttpURLConnection.HTTP_PARTIAL -> RangeCapability(
                    supportsRanges = true,
                    contentLength = totalFromContentRange(connection.getHeaderField("Content-Range")),
                    etag = etag,
                    lastModified = lastModified,
                )
                HttpURLConnection.HTTP_OK -> RangeCapability(
                    supportsRanges = false,
                    contentLength = connection.getHeaderFieldLong("Content-Length", -1L).takeIf { it >= 0 },
                    etag = etag,
                    lastModified = lastModified,
                )
                else -> throw HttpStatusException(status, url)
            }
        } finally {
            runCatching { connection.inputStream.close() }
            runCatching { connection.errorStream?.close() }
            connection.disconnect()
        }
    }

    private fun open(url: String, headers: Map<String, String>): HttpURLConnection =
        (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }

    private fun totalFromContentRange(value: String?): Long? =
        value?.substringAfterLast('/')?.toLongOrNull()
}

class HttpStatusException(val statusCode: Int, url: String) :
    java.io.IOException("HTTP $statusCode for $url")
