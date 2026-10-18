package app.novafetch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.novafetch.library.MediaItem

@Composable
fun LibraryScreen(
    items: List<MediaItem>,
    onPlay: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.padding(16.dp)) {
        items(items, key = { it.id }) { item ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(item) }
                    .padding(vertical = 12.dp),
            ) {
                Text(item.title)
                Text("${item.mediaType.name} • ${item.fileSize} bytes")
            }
        }
    }
}
