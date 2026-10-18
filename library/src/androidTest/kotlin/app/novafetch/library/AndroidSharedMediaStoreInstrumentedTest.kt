package app.novafetch.library

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class AndroidSharedMediaStoreInstrumentedTest {
    private lateinit var context: Context
    private lateinit var store: AndroidSharedMediaStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = AndroidSharedMediaStore(context.contentResolver)
    }

    @Test
    fun pendingRowCanBeWrittenCommittedAndRediscovered() = runBlocking {
        val unique = "novafetch-test-${System.nanoTime()}.bin"
        val metadata = FinalMediaMetadata(
            displayName = unique,
            mimeType = "application/octet-stream",
            relativePath = "Download/NovaFetch-Test",
        )
        val source = Files.createTempFile(context.cacheDir.toPath(), "novafetch-", ".bin")
        Files.write(source, byteArrayOf(1, 2, 3, 4))

        var uri: String? = null
        try {
            val pending = store.insertPending(metadata)
            uri = pending.uri
            store.write(pending.uri, source)
            store.commit(pending.uri)

            val found = store.inspect(pending.uri)
            assertNotNull(found)
            assertFalse(found!!.isPending)
        } finally {
            uri?.let { store.delete(it) }
            Files.deleteIfExists(source)
        }
    }
}
