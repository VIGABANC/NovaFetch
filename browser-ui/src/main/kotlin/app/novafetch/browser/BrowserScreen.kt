package app.novafetch.browser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.novafetch.download.DownloadPlan
import app.novafetch.resolver.DirectFormatResolver
import org.mozilla.geckoview.GeckoView

/** Light functional-first browser. Format sheet deliberately excludes manifests until Plan 03. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(manager: BrowserSessionManager, onDownload: (DownloadPlan.DirectPlan) -> Unit) {
    val tabs by manager.tabs.collectAsState()
    val active by manager.activeTab.collectAsState()
    val catalog by manager.media.collectAsState()
    var address by remember(active) { mutableStateOf(tabs.firstOrNull { it.id == active }?.url.orEmpty()) }
    var sheet by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = manager::back) { Text("‹") }
            TextButton(onClick = manager::forward) { Text("›") }
            TextButton(onClick = manager::reload) { Text("⟳") }
            OutlinedTextField(value = address, onValueChange = { address = it },
                singleLine = true, label = { Text("URL") }, modifier = Modifier.weight(1f))
            Button(onClick = { manager.navigate(address) }) { Text("Go") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { sheet = true }, enabled = catalog.options.isNotEmpty()) {
                Text("↓ ${catalog.options.size} Media")
            }
            TextButton(onClick = { manager.open("about:blank") }) { Text("+ Tab") }
            Text("Tabs: ${tabs.size}", modifier = Modifier.padding(top = 12.dp))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
            tabs.forEachIndexed { index, tab ->
                TextButton(onClick = { manager.select(tab.id) }) {
                    Text(if (tab.id == active) "● ${index + 1}" else "${index + 1}")
                }
                TextButton(onClick = { manager.close(tab.id) }) { Text("×") }
            }
        }
        val session = manager.session(active)
        if (session != null) {
            AndroidView(
                factory = { GeckoView(it).apply { setSession(session) } },
                update = { view -> if (manager.session(active) != null) view.setSession(manager.session(active)!!) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            Surface(Modifier.weight(1f).fillMaxWidth()) { Text("Opening browser…") }
        }
    }
    if (sheet) ModalBottomSheet(onDismissRequest = { sheet = false }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp)) {
            Text("Detected media · ${catalog.options.size}")
            catalog.options.forEach { option ->
                val plan = DirectFormatResolver.toPlan(option.value)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(option.candidate.title ?: option.candidate.kind.name)
                        Text(option.value.canonicalUrl.substringBefore('?').takeLast(54))
                    }
                    Button(onClick = { if (plan != null) { onDownload(plan); sheet = false } }, enabled = plan != null) {
                        Text(if (plan == null) "Later" else "Download")
                    }
                }
            }
        }
    }
}
