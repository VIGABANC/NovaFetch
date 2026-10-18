from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

def read(path):
    return (ROOT / path).read_text()

class AndroidAppContractTest(unittest.TestCase):
    def test_application_is_download_execution_host_and_recovers_jobs(self):
        src = read('app/src/main/kotlin/app/novafetch/NovaFetchApplication.kt')
        self.assertIn('DownloadExecutionHost', src)
        self.assertIn('executeDownload(', src)
        self.assertIn('recoverInterrupted()', src)

    def test_app_graph_wires_persistence_transfer_scheduler_and_mediastore(self):
        src = read('app/src/main/kotlin/app/novafetch/AppGraph.kt')
        for symbol in [
            'NovaFetchDatabaseFactory', 'RoomDownloadRepository', 'RoomMediaLibraryRepository',
            'AdaptiveDownloadScheduler', 'UidtDownloadScheduler', 'WorkManagerDownloadScheduler',
            'UrlConnectionTransferEngine', 'DirectDownloadRunner', 'AndroidSharedMediaStore',
            'MediaStoreSharedMediaPublisher', 'MediaFinalizer', 'DirectDownloadExecutionHost'
        ]:
            self.assertIn(symbol, src)

    def test_manifest_has_application_and_launcher_activity(self):
        src = read('app/src/main/AndroidManifest.xml')
        self.assertIn('android:name=".NovaFetchApplication"', src)
        self.assertIn('android:name=".MainActivity"', src)
        self.assertIn('android.intent.action.MAIN', src)
        self.assertIn('android.intent.category.LAUNCHER', src)

    def test_player_uses_media3_exoplayer_and_player_view(self):
        factory = read('player/src/main/kotlin/app/novafetch/player/PlayerFactory.kt')
        screen = read('player/src/main/kotlin/app/novafetch/player/PlayerScreen.kt')
        self.assertIn('ExoPlayer.Builder', factory)
        self.assertIn('setMediaItem', factory)
        self.assertIn('PlayerView', screen)
        self.assertIn('AndroidView', screen)

    def test_main_activity_imports_padding_extension(self):
        src = read('app/src/main/kotlin/app/novafetch/MainActivity.kt')
        self.assertIn('import androidx.compose.foundation.layout.padding', src)

    def test_compose_has_downloads_and_library_screens(self):
        self.assertIn('@Composable', read('app/src/main/kotlin/app/novafetch/DownloadsScreen.kt'))
        self.assertIn('@Composable', read('app/src/main/kotlin/app/novafetch/LibraryScreen.kt'))
        self.assertIn('setContent', read('app/src/main/kotlin/app/novafetch/MainActivity.kt'))

    def test_download_list_exposes_pause_and_resume_actions(self):
        downloads = read('app/src/main/kotlin/app/novafetch/DownloadsScreen.kt')
        activity = read('app/src/main/kotlin/app/novafetch/MainActivity.kt')
        self.assertIn('onPause: (DownloadId) -> Unit', downloads)
        self.assertIn('onResume: (DownloadId) -> Unit', downloads)
        self.assertIn('Text("Pause")', downloads)
        self.assertIn('Text("Resume")', downloads)
        self.assertIn('graph.coordinator.pause(id)', activity)
        self.assertIn('graph.coordinator.resume(id)', activity)

if __name__ == '__main__':
    unittest.main()
