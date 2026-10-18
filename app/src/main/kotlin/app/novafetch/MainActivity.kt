package app.novafetch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.novafetch.player.PlayerScreen
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = (application as NovaFetchApplication).graph
        setContent {
            MaterialTheme {
                NovaFetchRoot(graph)
            }
        }
    }
}

private enum class RootTab { DOWNLOADS, LIBRARY }

@Composable
private fun NovaFetchRoot(graph: AppGraph) {
    var tab by remember { mutableStateOf(RootTab.DOWNLOADS) }
    var directUrl by remember { mutableStateOf("") }
    var playingUri by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val jobs by graph.downloadListRepository.observeAll().collectAsState(initial = emptyList())
    val library by graph.mediaLibraryRepository.observeAll().collectAsState(initial = emptyList())

    if (playingUri != null) {
        Box(Modifier.fillMaxSize()) {
            PlayerScreen(playingUri!!, Modifier.fillMaxSize())
            Button(onClick = { playingUri = null }) { Text("Back") }
        }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == RootTab.DOWNLOADS,
                    onClick = { tab = RootTab.DOWNLOADS },
                    icon = { Text("↓") },
                    label = { Text("Downloads") },
                )
                NavigationBarItem(
                    selected = tab == RootTab.LIBRARY,
                    onClick = { tab = RootTab.LIBRARY },
                    icon = { Text("▶") },
                    label = { Text("Library") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                RootTab.DOWNLOADS -> DownloadsScreen(
                    url = directUrl,
                    onUrlChange = { directUrl = it; error = null },
                    onDownload = {
                        scope.launch {
                            runCatching { graph.enqueueDirect(directUrl) }
                                .onSuccess { directUrl = "" }
                                .onFailure { error = it.message ?: "Unable to start download" }
                        }
                    },
                    jobs = jobs,
                )
                RootTab.LIBRARY -> LibraryScreen(library, onPlay = { playingUri = it.contentUri })
            }
            error?.let { Text(it) }
        }
    }
}
