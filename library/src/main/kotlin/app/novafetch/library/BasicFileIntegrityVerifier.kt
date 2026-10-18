package app.novafetch.library

import app.novafetch.download.DownloadSnapshot
import java.nio.file.Files
import java.nio.file.Path

sealed interface FileVerificationResult {
    data object Valid : FileVerificationResult
    data class Invalid(val code: String) : FileVerificationResult
}

interface FileIntegrityVerifier {
    fun verify(snapshot: DownloadSnapshot, path: Path): FileVerificationResult
}

class BasicFileIntegrityVerifier : FileIntegrityVerifier {
    override fun verify(snapshot: DownloadSnapshot, path: Path): FileVerificationResult {
        if (!Files.isRegularFile(path)) return FileVerificationResult.Invalid("TEMP_FILE_MISSING")
        val size = Files.size(path)
        if (size <= 0L) return FileVerificationResult.Invalid("EMPTY_FILE")
        if (snapshot.totalBytes != null && size != snapshot.totalBytes) {
            return FileVerificationResult.Invalid("SIZE_MISMATCH")
        }
        if (snapshot.downloadedBytes != size) {
            return FileVerificationResult.Invalid("SIZE_MISMATCH")
        }
        return FileVerificationResult.Valid
    }
}
