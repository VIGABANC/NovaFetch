package app.novafetch.download

import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class UrlConnectionTransferEngine(
    private val rangeProbe: RangeProbe,
) : TransferEngine {
    override suspend fun transfer(
        request: TransferRequest,
        onProgress: suspend (downloadedBytes: Long) -> Unit,
    ): TransferResult {
        val capability = rangeProbe.probe(request.url, request.headers)
        if (request.resumeOffset > 0 && sourceChanged(request, capability)) {
            return TransferResult.SourceChanged(capability.etag, capability.lastModified)
        }

        Files.createDirectories(request.destination.toAbsolutePath().parent)

        val resume = request.resumeOffset > 0 && capability.supportsRanges
        val restart = request.resumeOffset > 0 && !capability.supportsRanges

        if (resume) {
            require(Files.exists(request.destination)) { "resume destination does not exist" }
            require(Files.size(request.destination) == request.resumeOffset) {
                "partial file size does not match resumeOffset"
            }
        }

        val connection = open(request)
        if (resume) {
            connection.setRequestProperty("Range", "bytes=${request.resumeOffset}-")
            (request.etag ?: request.lastModified)?.let { connection.setRequestProperty("If-Range", it) }
        }

        return try {
            val status = connection.responseCode
            if (resume && status != HttpURLConnection.HTTP_PARTIAL) {
                if (status == HttpURLConnection.HTTP_OK) {
                    connection.disconnect()
                    return transferFromBeginning(request, onProgress, restarted = true)
                }
                throw HttpStatusException(status, request.url)
            }
            if (!resume && status != HttpURLConnection.HTTP_OK) {
                throw HttpStatusException(status, request.url)
            }

            if (resume) {
                val contentRange = parseContentRange(connection.getHeaderField("Content-Range"))
                    ?: throw InvalidContentRangeException()
                if (contentRange.start != request.resumeOffset ||
                    (capability.contentLength != null && contentRange.total != capability.contentLength) ||
                    contentRange.end != contentRange.total - 1
                ) {
                    throw InvalidContentRangeException()
                }
                val responseEtag = connection.getHeaderField("ETag")
                val responseModified = connection.getHeaderField("Last-Modified")
                if ((request.etag != null && responseEtag != request.etag) ||
                    (request.etag == null && request.lastModified != null && responseModified != request.lastModified)
                ) {
                    return TransferResult.SourceChanged(responseEtag, responseModified)
                }
                val responseLength = connection.getHeaderFieldLong("Content-Length", -1L)
                if (responseLength >= 0 && responseLength != contentRange.end - contentRange.start + 1) {
                    throw InvalidContentRangeException()
                }
            }

            val options = if (resume) {
                arrayOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)
            } else {
                arrayOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
            }
            var downloaded = if (resume) request.resumeOffset else 0L
            var bodyBytes = 0L
            Files.newOutputStream(request.destination, *options).use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        bodyBytes += count
                        onProgress(downloaded)
                    }
                }
            }
            TransferResult.Completed(
                bytesWritten = bodyBytes,
                totalBytes = capability.contentLength,
                supportsResume = capability.supportsRanges,
                resumed = resume,
                restarted = restart,
                etag = connection.getHeaderField("ETag") ?: capability.etag,
                lastModified = connection.getHeaderField("Last-Modified") ?: capability.lastModified,
            )
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun transferFromBeginning(
        request: TransferRequest,
        onProgress: suspend (Long) -> Unit,
        restarted: Boolean,
    ): TransferResult {
        val fresh = request.copy(resumeOffset = 0, etag = null, lastModified = null)
        val result = transfer(fresh, onProgress)
        return if (result is TransferResult.Completed && restarted) result.copy(restarted = true) else result
    }

    private fun sourceChanged(request: TransferRequest, capability: RangeCapability): Boolean {
        val etagChanged = request.etag != null && request.etag != capability.etag
        val modifiedChanged = request.etag == null && request.lastModified != null &&
            request.lastModified != capability.lastModified
        return etagChanged || modifiedChanged
    }

    private fun parseContentRange(value: String?): ByteRange? {
        val match = CONTENT_RANGE.matchEntire(value ?: return null) ?: return null
        val start = match.groupValues[1].toLongOrNull() ?: return null
        val end = match.groupValues[2].toLongOrNull() ?: return null
        val total = match.groupValues[3].toLongOrNull() ?: return null
        if (start > end || end >= total) return null
        return ByteRange(start, end, total)
    }

    private data class ByteRange(val start: Long, val end: Long, val total: Long)

    private companion object {
        val CONTENT_RANGE = Regex("bytes (\\d+)-(\\d+)/(\\d+)")
    }

    private fun open(request: TransferRequest): HttpURLConnection =
        (URI(request.url).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
            request.headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }
}

class InvalidContentRangeException : java.io.IOException("Server returned an invalid byte range")
