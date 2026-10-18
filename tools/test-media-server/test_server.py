import http.client
import threading
import unittest
from pathlib import Path

import server


class MediaServerTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.httpd = server.create_server("127.0.0.1", 0)
        cls.port = cls.httpd.server_address[1]
        cls.thread = threading.Thread(target=cls.httpd.serve_forever, daemon=True)
        cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.httpd.shutdown()
        cls.httpd.server_close()
        cls.thread.join(timeout=2)

    def request(self, path, headers=None):
        conn = http.client.HTTPConnection("127.0.0.1", self.port, timeout=2)
        conn.request("GET", path, headers=headers or {})
        response = conn.getresponse()
        body = response.read()
        result = response.status, dict(response.getheaders()), body
        conn.close()
        return result

    def test_browser_media_fixture_page(self):
        status, headers, body = self.request("/media-page.html")
        self.assertEqual(200, status)
        self.assertIn("text/html", headers.get("Content-Type", ""))
        self.assertIn(b'<video', body)
        self.assertIn(b'/playable.mp4', body)

    def test_range_endpoint_returns_partial_content(self):
        status, headers, body = self.request("/range.mp4", {"Range": "bytes=0-9"})
        self.assertEqual(206, status)
        self.assertEqual("bytes 0-9/65536", headers["Content-Range"])
        self.assertEqual(10, len(body))

    def test_no_range_endpoint_ignores_range_and_returns_full_body(self):
        status, headers, body = self.request("/no-range.mp4", {"Range": "bytes=0-9"})
        self.assertEqual(200, status)
        self.assertEqual(65536, len(body))
        self.assertNotIn("Content-Range", headers)

    def test_redirect_endpoint_redirects_to_direct_media(self):
        status, headers, _ = self.request("/redirect.mp4")
        self.assertEqual(302, status)
        self.assertEqual("/direct.mp4", headers["Location"])

    def test_error_503_is_deterministic(self):
        status, _, body = self.request("/error-503")
        self.assertEqual(503, status)
        self.assertEqual(b"temporary failure", body)

    def test_direct_endpoint_returns_full_fixture(self):
        status, headers, body = self.request("/direct.mp4")
        self.assertEqual(200, status)
        self.assertEqual("video/mp4", headers["Content-Type"])
        self.assertEqual(65536, len(body))

    def test_playable_endpoint_serves_valid_mp4_fixture(self):
        status, headers, body = self.request("/playable.mp4")
        self.assertEqual(200, status)
        self.assertEqual("video/mp4", headers["Content-Type"])
        self.assertEqual((Path(__file__).with_name("fixtures") / "playable.mp4").read_bytes(), body)

    def test_playable_fixture_can_be_slow_and_still_support_ranges(self):
        self.assertEqual(204, self.request("/control/slow-playable?enabled=1")[0])
        try:
            status, headers, body = self.request("/playable.mp4", {"Range": "bytes=0-9"})
            fixture = (Path(__file__).with_name("fixtures") / "playable.mp4").read_bytes()
            self.assertEqual(206, status)
            self.assertEqual(f"bytes 0-9/{len(fixture)}", headers["Content-Range"])
            self.assertEqual(fixture[:10], body)
        finally:
            self.assertEqual(204, self.request("/control/slow-playable?enabled=0")[0])

    def test_expired_url_requires_fresh_token(self):
        status, _, _ = self.request("/expired-url")
        self.assertEqual(403, status)
        status, _, body = self.request("/expired-url?token=fresh")
        self.assertEqual(200, status)
        self.assertEqual(65536, len(body))

    def test_disconnect_endpoint_terminates_before_declared_length(self):
        conn = http.client.HTTPConnection("127.0.0.1", self.port, timeout=2)
        conn.request("GET", "/disconnect-mid-download")
        response = conn.getresponse()
        self.assertEqual(200, response.status)
        self.assertEqual("65536", response.getheader("Content-Length"))
        with self.assertRaises(http.client.IncompleteRead) as caught:
            response.read()
        self.assertGreater(len(caught.exception.partial), 0)
        self.assertLess(len(caught.exception.partial), 65536)
        conn.close()

    def test_range_endpoint_exposes_stable_etag(self):
        status, headers, _ = self.request("/range.mp4", {"Range": "bytes=0-0"})
        self.assertEqual(206, status)
        self.assertEqual('"fixture-v1"', headers["ETag"])

    def test_etag_can_change_and_if_range_falls_back_to_full_response(self):
        status, _, _ = self.request("/control/etag?value=fixture-v2")
        self.assertEqual(204, status)
        status, headers, body = self.request(
            "/range.mp4",
            {"Range": "bytes=100-199", "If-Range": '"fixture-v1"'},
        )
        self.assertEqual(200, status)
        self.assertEqual('"fixture-v2"', headers["ETag"])
        self.assertEqual(65536, len(body))
        self.request("/control/etag?value=fixture-v1")


if __name__ == "__main__":
    unittest.main()
