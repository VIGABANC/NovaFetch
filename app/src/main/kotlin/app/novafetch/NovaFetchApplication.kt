package app.novafetch

import android.app.Application
import app.novafetch.common.DownloadId
import app.novafetch.download.DownloadExecutionHost
import app.novafetch.download.DownloadExecutionOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NovaFetchApplication : Application(), DownloadExecutionHost {
    lateinit var graph: AppGraph
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        applicationScope.launch {
            graph.coordinator.recoverInterrupted()
        }
    }

    override suspend fun executeDownload(id: DownloadId): DownloadExecutionOutcome =
        graph.executionHost.executeDownload(id)
}
