package app.novafetch.browser

import android.content.Context
import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension
import app.novafetch.resolver.CandidateValidator
import app.novafetch.resolver.MediaCatalog
import app.novafetch.resolver.CatalogSnapshot
import app.novafetch.resolver.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Android host for per-tab GeckoSession and native media-candidate messages. */
class BrowserSessionManager(context: Context) {
    private val runtime: GeckoRuntime = GeckoRuntime.create(context.applicationContext)
    private val registry = BrowserTabRegistry()
    private val sessions = mutableMapOf<BrowserTabId, GeckoSession>()
    private val catalogs = mutableMapOf<BrowserTabId, MediaCatalog>()
    private val validator = ExtensionMessageValidator()
    private val candidateValidator = CandidateValidator()
    private var detectorExtension: WebExtension? = null
    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    private val _active = MutableStateFlow<BrowserTabId?>(null)
    private val _media = MutableStateFlow(CatalogSnapshot(emptyList()))
    val tabs: StateFlow<List<BrowserTab>> = _tabs
    val activeTab: StateFlow<BrowserTabId?> = _active
    val media: StateFlow<CatalogSnapshot> = _media

    init {
        open("about:blank")
        runtime.webExtensionController.ensureBuiltIn(
            "resource://android/assets/extensions/media-detector/",
            ExtensionMessageValidator.EXTENSION_ID,
        ).accept({ extension ->
            detectorExtension = extension
            sessions.forEach { (id, session) -> attach(id, session, extension) }
        }, { _ ->
            // The browser must remain usable even when detector extension installation fails.
        })
    }

    fun open(url: String): BrowserTabId {
        val id = registry.open(url)
        val session = GeckoSession()
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean,
            ) {
                if (url != null) {
                    registry.navigate(id, url)
                    catalogs[id]?.clear()
                    publish()
                }
            }
        }
        session.open(runtime)
        sessions[id] = session
        catalogs[id] = MediaCatalog()
        detectorExtension?.let { attach(id, session, it) }
        publish()
        if (url != "about:blank") session.loadUri(url)
        return id
    }

    fun session(id: BrowserTabId?): GeckoSession? = id?.let { sessions[it] }

    fun navigate(input: String) {
        val destination = normalizeNavigation(input) ?: return
        val id = _active.value ?: open(destination)
        catalogs[id]?.clear()
        _media.value = catalogs[id]?.snapshot() ?: CatalogSnapshot(emptyList())
        sessions[id]?.loadUri(destination)
    }

    fun select(id: BrowserTabId) { registry.activate(id); publish() }
    fun close(id: BrowserTabId) {
        sessions.remove(id)?.close()
        catalogs.remove(id)
        registry.close(id)
        if (registry.activeId() == null) open("about:blank") else publish()
    }
    fun back() { session(_active.value)?.goBack() }
    fun forward() { session(_active.value)?.goForward() }
    fun reload() { session(_active.value)?.reload() }

    private fun attach(id: BrowserTabId, session: GeckoSession, extension: WebExtension) {
        session.webExtensionController.setMessageDelegate(extension, object : WebExtension.MessageDelegate {
            override fun onMessage(
                nativeApp: String,
                message: Any,
                sender: WebExtension.MessageSender,
            ): GeckoResult<Any>? {
                if (nativeApp != NATIVE_APP || sender.session !== session || sender.webExtension.id != ExtensionMessageValidator.EXTENSION_ID) return null
                val payload = message as? JSONObject ?: return null
                if (payload.toString().length > 32_000) return null
                val parsed = parseMessage(payload) ?: return null
                val checked = validator.validate(
                    ExtensionSenderContext(sender.webExtension.id, true, sender.url), parsed,
                ).getOrNull() ?: return null
                if (sessions[id] !== session) return null
                val catalog = catalogs[id] ?: return null
                checked.forEach { candidate ->
                    when (val verified = candidateValidator.validate(candidate)) {
                        is ValidationResult.Accepted -> catalog.upsert(verified.value)
                        is ValidationResult.Rejected -> Unit
                    }
                }
                if (_active.value == id) _media.value = catalog.snapshot()
                return null
            }
        }, NATIVE_APP)
    }

    private fun parseMessage(json: JSONObject): DomMediaMessage? = runCatching {
        val list = json.getJSONArray("items")
        require(list.length() in 1..10)
        DomMediaMessage(
            json.getInt("schemaVersion"), json.getString("type"), json.getString("pageUrl"),
            (0 until list.length()).map { index ->
                val item = list.getJSONObject(index)
                DomMediaHint(
                    item.getString("url"), item.getString("kind"),
                    item.getString("method"), item.optString("mimeType", ""),
                )
            },
        )
    }.getOrNull()

    private fun publish() {
        _tabs.value = registry.list()
        _active.value = registry.activeId()
        _media.value = catalogs[_active.value]?.snapshot() ?: CatalogSnapshot(emptyList())
    }

    companion object {
        const val NATIVE_APP = "app.novafetch.media"
        fun normalizeNavigation(raw: String): String? {
            val value = raw.trim()
            if (value.isBlank() || value.length > 2048) return null
            if (value.startsWith("http://", true) || value.startsWith("https://", true)) return value
            if (value.contains(' ') || value.contains('/') && !value.contains('.')) return null
            return "https://$value"
        }
    }
}
