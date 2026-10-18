package app.novafetch.download

import app.novafetch.common.DownloadId
import java.nio.file.Path

fun interface DownloadTempPathProvider {
    fun pathFor(id: DownloadId, fileName: String): Path
}
