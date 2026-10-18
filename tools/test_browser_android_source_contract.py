import json
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent

class BrowserAndroidSourceContract(unittest.TestCase):
    def test_mozilla_stable_dependency_and_modules(self):
        gradle = (ROOT / 'gradle/libs.versions.toml').read_text()
        self.assertIn('org.mozilla.geckoview:geckoview', gradle)
        settings = (ROOT / 'settings.gradle.kts').read_text()
        for module in (':browser-core', ':browser-ui', ':media-detection', ':media-resolver'):
            self.assertIn(module, settings)

    def test_built_in_extension_permissions_and_bridge(self):
        manifest = json.loads((ROOT / 'browser-core/src/main/assets/extensions/media-detector/manifest.json').read_text())
        self.assertEqual(manifest['browser_specific_settings']['gecko']['id'], 'media-detector@novafetch.app')
        self.assertIn('nativeMessaging', manifest['permissions'])
        self.assertIn('nativeMessagingFromContent', manifest['permissions'])
        bridge = (ROOT / 'browser-core/src/main/kotlin/app/novafetch/browser/BrowserSessionManager.kt').read_text()
        self.assertIn('ensureBuiltIn(', bridge)
        self.assertIn('sender.session !== session', bridge)
        self.assertIn('sender.webExtension.id !=', bridge)
        self.assertIn('catalog.upsert(', bridge)

    def test_device_verification_runs_fixture_server_and_app_tests(self):
        script = (ROOT / 'tools/verify-android-runtime.sh').read_text()
        self.assertIn('test-media-server/server.py', script)
        self.assertIn(':app:connectedDebugAndroidTest', script)
        build = (ROOT / 'tools/verify-android-build.sh').read_text()
        self.assertIn(':app:compileDebugAndroidTestKotlin', build)

    def test_http_browser_and_direct_media_are_allowed(self):
        manifest = (ROOT / 'app/src/main/AndroidManifest.xml').read_text()
        self.assertIn('android:usesCleartextTraffic="true"', manifest)

    def test_manifest_sources_only_and_no_code_to_bypass_drm(self):
        manifest = json.loads((ROOT / 'browser-core/src/main/assets/extensions/media-detector/manifest.json').read_text())
        self.assertEqual(manifest['content_scripts'][0]['matches'], ['http://*/*', 'https://*/*'])
        self.assertEqual(manifest['content_scripts'][0]['all_frames'], True)
        self.assertNotIn('downloads', manifest['permissions'])

if __name__ == '__main__': unittest.main()
