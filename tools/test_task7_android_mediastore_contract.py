from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'library/src/main/kotlin/app/novafetch/library/AndroidSharedMediaStore.kt'

class AndroidMediaStoreContractTest(unittest.TestCase):
    def test_uses_pending_atomic_publish_and_relative_path(self):
        src = SOURCE.read_text()
        self.assertIn('MediaStore.MediaColumns.IS_PENDING', src)
        self.assertIn('MediaStore.MediaColumns.RELATIVE_PATH', src)
        self.assertIn('MediaStore.MediaColumns.DISPLAY_NAME', src)
        self.assertIn('openOutputStream(', src)
        self.assertIn('resolver.insert(collection, values)', src)

    def test_inspects_exact_persisted_uri_for_recovery(self):
        source = SOURCE.read_text()
        self.assertIn('override suspend fun inspect(uri: String)', source)
        self.assertIn('val parsed = Uri.parse(uri)', source)
        self.assertIn('resolver.query(parsed, projection', source)

    def test_selects_media_collection_by_mime_type(self):
        src = SOURCE.read_text()
        self.assertIn('MediaStore.Video.Media.EXTERNAL_CONTENT_URI', src)
        self.assertIn('MediaStore.Audio.Media.EXTERNAL_CONTENT_URI', src)
        self.assertIn('MediaStore.Images.Media.EXTERNAL_CONTENT_URI', src)
        self.assertIn('MediaStore.Downloads.EXTERNAL_CONTENT_URI', src)

if __name__ == '__main__':
    unittest.main()
