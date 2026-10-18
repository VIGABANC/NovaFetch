package app.novafetch.browser

import java.util.UUID

@JvmInline value class BrowserTabId(val value: String)
data class BrowserTab(val id: BrowserTabId, val url: String)

/** Does not own GeckoSessions. Keeps user-facing tab identity distinct from runtime handles. */
class BrowserTabRegistry(private val newId: () -> String = { UUID.randomUUID().toString() }) {
    private val tabs = linkedMapOf<BrowserTabId, BrowserTab>()
    private var active: BrowserTabId? = null
    fun open(url: String): BrowserTabId {
        val id = BrowserTabId(newId())
        tabs[id] = BrowserTab(id, url)
        active = id
        return id
    }
    fun list(): List<BrowserTab> = tabs.values.toList()
    fun activeId(): BrowserTabId? = active
    fun activate(id: BrowserTabId) { require(id in tabs); active = id }
    fun navigate(id: BrowserTabId, url: String) { require(id in tabs); tabs[id] = BrowserTab(id, url) }
    fun close(id: BrowserTabId) {
        tabs.remove(id)
        if (active == id) active = tabs.keys.lastOrNull()
    }
}
