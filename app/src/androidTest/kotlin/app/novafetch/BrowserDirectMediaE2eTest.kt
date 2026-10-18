package app.novafetch

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Requires tools/test-media-server/server.py on the emulator host at port 8765. */
@RunWith(AndroidJUnit4::class)
class BrowserDirectMediaE2eTest {
    @Test fun detectsAndDownloadsPlayableMp4() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val graph = (instrumentation.targetContext.applicationContext as NovaFetchApplication).graph
        val activity = ActivityScenario.launch(MainActivity::class.java)
        try {
            instrumentation.runOnMainSync {
                graph.browserManager.navigate("http://10.0.2.2:8765/media-page.html")
            }
        val deadline = System.currentTimeMillis() + 30_000
        var options = graph.browserManager.media.value.options
        while (options.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(200)
            options = graph.browserManager.media.value.options
        }
        assertTrue("GeckoView extension did not detect fixture MP4", options.any {
            it.value.canonicalUrl.endsWith("/playable.mp4")
        })
        val video = options.first { it.value.canonicalUrl.endsWith("/playable.mp4") }
        val plan = requireNotNull(app.novafetch.resolver.DirectFormatResolver.toPlan(video.value))
        val id = runBlocking { graph.coordinator.enqueue(plan) }
        assertNotNull(id.value)
        // The actual download completion is asserted by the Plan 01 Android E2E/recovery gate.
        } finally {
            activity.close()
        }
    }
}
