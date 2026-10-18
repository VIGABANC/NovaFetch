package app.novafetch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.novafetch.download.DownloadSnapshot

@Composable
fun DownloadsScreen(
    url: String,
    onUrlChange: (String) -> Unit,
    onDownload: () -> Unit,
    jobs: List<DownloadSnapshot>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Direct download")
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("https://…/file.mp4") },
        )
        Button(onClick = onDownload, enabled = url.startsWith("http")) {
            Text("Download")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(jobs, key = { it.id.value }) { job ->
                val total = job.totalBytes
                val progress = if (total != null && total > 0L) {
                    (job.downloadedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                } else null
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(job.plan.suggestedFileName)
                        Text(job.state.name)
                    }
                    if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("${job.downloadedBytes} bytes")
                }
            }
        }
    }
}
