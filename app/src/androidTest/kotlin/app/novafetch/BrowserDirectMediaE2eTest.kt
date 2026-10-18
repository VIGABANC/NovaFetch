package app.novafetch

import android.net.Uri
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.novafetch.common.DownloadId
import app.novafetch.common.DownloadState
import app.novafetch.download.DownloadPlan
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Requires tools/test-media-server/server.py on the emulator host at port 8765. */
@RunWith(AndroidJUnit4::class)
class BrowserDirectMediaE2eTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun detectsPausesResumesFinalizesListsAndPlaysFixtureOffline() {
        val activity = compose.activity
        val graph = (activity.application as NovaFetchApplication).graph
        setSlowFixture(enabled = true)
        try {
            activity.runOnUiThread {
                graph.browserManager.navigate("http://10.0.2.2:8765/media-page.html")
            }
            compose.waitUntil(30_000) {
                graph.browserManager.media.value.options.any {
                    it.value.canonicalUrl.endsWith("/playable.mp4")
                }
            }

            val optionCount = graph.browserManager.media.value.options.size
            assertTrue("fixture must expose a direct MP4 choice", optionCount > 0)
            compose.onNodeWithText("↓ $optionCount Media").performClick()
            compose.onNodeWithText("Detected media", substring = true).assertExists()
            compose.onNodeWithText("Download").performClick()

            var downloadId: DownloadId? = null
            compose.waitUntil(30_000) {
                runBlocking {
                    graph.downloadRepository.unfinished()
                        .firstOrNull { (it.plan as? DownloadPlan.DirectPlan)?.sourceUrl?.endsWith("/playable.mp4") == true }
                        ?.let { downloadId = it.id; true } ?: false
                }
            }
            val id = requireNotNull(downloadId)
            compose.onNodeWithText("Downloads").performClick()
            compose.waitUntil(30_000) {
                runBlocking {
                    graph.downloadRepository.get(id)?.let {
                        it.state == DownloadState.DOWNLOADING && it.downloadedBytes > 0L
                    } == true
                }
            }

            compose.onNodeWithText("Pause").performClick()
            compose.waitUntil(10_000) {
                runBlocking { graph.downloadRepository.get(id)?.state == DownloadState.PAUSED }
            }
            val paused = requireNotNull(runBlocking { graph.downloadRepository.get(id) })
            assertTrue("pause must preserve a non-empty partial file", paused.downloadedBytes > 0)
            assertEquals(
                paused.downloadedBytes,
                java.nio.file.Files.size(java.nio.file.Path.of(requireNotNull(paused.tempPath))),
            )

            compose.onNodeWithText("Resume").performClick()
            compose.waitUntil(60_000) {
                runBlocking { graph.downloadRepository.get(id)?.state == DownloadState.COMPLETED }
            }
            val completed = requireNotNull(runBlocking { graph.downloadRepository.get(id) })
            assertNotNull("completion must have a MediaStore URI", completed.outputUri)
            val storedBytes = activity.contentResolver.openInputStream(Uri.parse(completed.outputUri!!))!!
                .use { it.readBytes() }
            val expectedBytes = InstrumentationRegistry.getInstrumentation().context.assets
                .open("playable.mp4").use { it.readBytes() }
            assertArrayEquals("MediaStore content must match the source bytes", expectedBytes, storedBytes)

            val libraryItem = runBlocking { graph.mediaLibraryRepository.getByDownloadId(id) }
            assertNotNull("completed download must appear in Library", libraryItem)
            assertEquals(completed.outputUri, libraryItem!!.contentUri)
            compose.onNodeWithText("Library").performClick()
            compose.onNodeWithText(libraryItem.title).assertExists()
            compose.onNodeWithText(libraryItem.title).performClick()

            assertOfflinePlayback(activity, completed.outputUri!!)
        } finally {
            setSlowFixture(enabled = false)
        }
    }

    private fun setSlowFixture(enabled: Boolean) {
        val connection = URL(
            "http://10.0.2.2:8765/control/slow-playable?enabled=${if (enabled) 1 else 0}",
        ).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            assertEquals(204, connection.responseCode)
        } finally {
            connection.disconnect()
        }
    }

    private fun assertOfflinePlayback(activity: MainActivity, contentUri: String) {
        val ended = CountDownLatch(1)
        val playbackError = AtomicReference<PlaybackException?>()
        val player = ExoPlayer.Builder(activity).build()
        try {
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) ended.countDown()
                }

                override fun onPlayerError(error: PlaybackException) {
                    playbackError.set(error)
                    ended.countDown()
                }
            })
            player.setMediaItem(MediaItem.fromUri(Uri.parse(contentUri)))
            player.prepare()
            player.playWhenReady = true
            assertTrue("MediaStore file must play to completion offline", ended.await(30, TimeUnit.SECONDS))
            assertNull("offline playback error", playbackError.get())
        } finally {
            player.release()
        }
    }
}
