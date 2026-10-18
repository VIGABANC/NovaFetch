import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
METADATA_PATH = ROOT / "app/build/outputs/apk/debug/output-metadata.json"


@unittest.skipUnless(METADATA_PATH.exists(), "assembleDebug must run before checking APK outputs")
class ApkAbiSplitTest(unittest.TestCase):
    def test_debug_build_contains_abi_apks_and_universal_apk(self):
        metadata_path = METADATA_PATH
        self.assertTrue(metadata_path.exists(), "assembleDebug must produce output metadata")
        metadata = json.loads(metadata_path.read_text())
        elements = metadata["elements"]
        abis = {
            next((item["value"] for item in element.get("filters", []) if item["filterType"] == "ABI"), None)
            for element in elements
        }
        self.assertTrue({"arm64-v8a", "armeabi-v7a", "x86_64"}.issubset(abis))
        self.assertIn(None, abis, "keep the universal debug APK alongside ABI-specific APKs")
        for element in elements:
            self.assertTrue((metadata_path.parent / element["outputFile"]).exists())


if __name__ == "__main__":
    unittest.main()
