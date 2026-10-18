from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

def read(path: str) -> str:
    return (ROOT / path).read_text()

class AndroidInstrumentationContractTest(unittest.TestCase):
    def test_version_catalog_declares_android_test_stack(self):
        catalog = read('gradle/libs.versions.toml')
        for token in [
            'androidxTestCore', 'androidxTestExtJunit', 'androidxTestRunner',
            'androidx-test-core', 'androidx-test-ext-junit', 'androidx-test-runner',
            'androidx-work-testing',
        ]:
            self.assertIn(token, catalog)

    def test_data_module_has_real_room_instrumentation_test(self):
        path = ROOT / 'data/src/androidTest/kotlin/app/novafetch/data/NovaFetchDatabaseInstrumentedTest.kt'
        self.assertTrue(path.exists())
        source = path.read_text()
        self.assertIn('Room.inMemoryDatabaseBuilder', source)
        self.assertIn('DownloadJobEntity', source)
        self.assertIn('MediaItemEntity', source)

    def test_library_module_has_real_mediastore_instrumentation_test(self):
        path = ROOT / 'library/src/androidTest/kotlin/app/novafetch/library/AndroidSharedMediaStoreInstrumentedTest.kt'
        self.assertTrue(path.exists())
        source = path.read_text()
        self.assertIn('AndroidSharedMediaStore', source)
        self.assertIn('insertPending', source)
        self.assertIn('commit', source)
        self.assertIn('inspect', source)

    def test_download_module_has_workmanager_instrumentation_test(self):
        path = ROOT / 'download-android/src/androidTest/kotlin/app/novafetch/download/WorkManagerDownloadSchedulerInstrumentedTest.kt'
        self.assertTrue(path.exists())
        source = path.read_text()
        self.assertIn('WorkManagerTestInitHelper', source)
        self.assertIn('WorkManagerDownloadScheduler', source)
        self.assertIn('getWorkInfosByTag', source)

    def test_modules_depend_on_android_test_libraries(self):
        data = read('data/build.gradle.kts')
        library = read('library/build.gradle.kts')
        download = read('download-android/build.gradle.kts')
        for source in [data, library, download]:
            self.assertIn('androidTestImplementation(libs.androidx.test.ext.junit)', source)
            self.assertIn('androidTestImplementation(libs.androidx.test.runner)', source)
        self.assertIn('androidTestImplementation(libs.androidx.work.testing)', download)

if __name__ == '__main__':
    unittest.main()
