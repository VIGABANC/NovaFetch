package app.novafetch.browser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserSessionManagerInstrumentedTest {
    @Test fun twoTabsHaveIndependentGeckoSessions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val manager = BrowserSessionManager(instrumentation.targetContext)
            val one = manager.open("about:blank")
            val two = manager.open("about:blank")
            assertNotSame(manager.session(one), manager.session(two))
            assertEquals(two, manager.activeTab.value)
            assertTrue(manager.tabs.value.size >= 3) // startup blank + 2 new tabs
            manager.close(one)
            assertNull(manager.session(one))
            manager.close(two)
        }
    }
}
