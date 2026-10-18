from __future__ import annotations

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlsplit
import re
import time

FIXTURE = Path(__file__).with_name("fixtures") / "direct.mp4"
PLAYABLE_FIXTURE = Path(__file__).with_name("fixtures") / "playable.mp4"
_RANGE_RE = re.compile(r"^bytes=(\d+)-(\d*)$")


class MediaRequestHandler(BaseHTTPRequestHandler):
    server_version = "NovaFetchTestMedia/0.1"

    def log_message(self, format, *args):
        return

    def do_GET(self):
        parsed = urlsplit(self.path)
        path = parsed.path

        if path == "/media-page.html":
            data = Path(__file__).with_name("fixtures").joinpath("media-page.html").read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
            return

        if path == "/control/etag":
            value = parse_qs(parsed.query).get("value", [None])[0]
            if not value:
                self.send_error(400)
                return
            self.server.etag_value = value
            self.send_response(204)
            self.end_headers()
            return

        if path == "/control/slow-playable":
            enabled = parse_qs(parsed.query).get("enabled", ["0"])[0]
            self.server.slow_playable = enabled == "1"
            self.send_response(204)
            self.end_headers()
            return

        if path == "/disconnect-mid-download":
            data = FIXTURE.read_bytes()
            halfway = len(data) // 2
            self.send_response(200)
            self.send_header("Content-Type", "video/mp4")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data[:halfway])
            self.wfile.flush()
            self.close_connection = True
            return

        if path == "/disconnect-once.mp4":
            data = FIXTURE.read_bytes()
            range_header = self.headers.get("Range")
            if range_header:
                self._send_range(data, range_header)
            elif not self.server.disconnected_once:
                self.server.disconnected_once = True
                halfway = len(data) // 2
                self.send_response(200)
                self.send_header("Content-Type", "video/mp4")
                self.send_header("Content-Length", str(len(data)))
                self.send_header("Accept-Ranges", "bytes")
                self.send_header("ETag", self._etag())
                self.end_headers()
                self.wfile.write(data[:halfway])
                self.wfile.flush()
                self.close_connection = True
            else:
                self._send_full(data, advertise_range=True)
            return

        if path == "/expired-url":
            token = parse_qs(parsed.query).get("token", [None])[0]
            if token != "fresh":
                body = b"expired"
                self.send_response(403)
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)
                return
            self._send_full(FIXTURE.read_bytes(), advertise_range=True)
            return

        if path == "/redirect.mp4":
            self.send_response(302)
            self.send_header("Location", "/direct.mp4")
            self.send_header("Content-Length", "0")
            self.end_headers()
            return

        if path == "/error-503":
            body = b"temporary failure"
            self.send_response(503)
            self.send_header("Content-Type", "text/plain")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        if path in {"/direct.mp4", "/no-range.mp4"}:
            self._send_full(FIXTURE.read_bytes(), advertise_range=False)
            return

        if path == "/playable.mp4":
            data = PLAYABLE_FIXTURE.read_bytes()
            if self.server.slow_playable:
                self._send_slow_playable(data)
            else:
                self._send_full(data, advertise_range=True)
            return

        if path == "/range.mp4":
            data = FIXTURE.read_bytes()
            range_header = self.headers.get("Range")
            if_range = self.headers.get("If-Range")
            current_etag = self._etag()
            if range_header and if_range and if_range != current_etag:
                self._send_full(data, advertise_range=True)
            elif range_header:
                self._send_range(data, range_header)
            else:
                self._send_full(data, advertise_range=True)
            return

        if path == "/wrong-range.mp4":
            data = FIXTURE.read_bytes()
            range_header = self.headers.get("Range")
            match = _RANGE_RE.match(range_header or "")
            if not match:
                self.send_error(416)
                return
            start = int(match.group(1))
            end = len(data) - 1
            body = data[start : end + 1]
            self.send_response(206)
            self.send_header("Content-Type", "video/mp4")
            self.send_header("Content-Range", f"bytes {start + 1}-{end}/{len(data)}")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("ETag", self._etag())
            self.end_headers()
            self.wfile.write(body)
            return

        self.send_error(404)

    def _etag(self) -> str:
        return f'"{getattr(self.server, "etag_value", "fixture-v1")}"'

    def _send_full(self, data: bytes, advertise_range: bool):
        self.send_response(200)
        self.send_header("Content-Type", "video/mp4")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("ETag", self._etag())
        if advertise_range:
            self.send_header("Accept-Ranges", "bytes")
        self.end_headers()
        self.wfile.write(data)

    def _send_range(self, data: bytes, value: str):
        match = _RANGE_RE.match(value)
        if not match:
            self.send_error(416)
            return
        start = int(match.group(1))
        end = int(match.group(2)) if match.group(2) else len(data) - 1
        if start >= len(data) or start > end:
            self.send_error(416)
            return
        end = min(end, len(data) - 1)
        body = data[start : end + 1]
        self.send_response(206)
        self.send_header("Content-Type", "video/mp4")
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Range", f"bytes {start}-{end}/{len(data)}")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("ETag", self._etag())
        self.end_headers()
        self.wfile.write(body)

    def _send_slow_playable(self, data: bytes):
        range_header = self.headers.get("Range")
        if range_header:
            match = _RANGE_RE.match(range_header)
            if not match:
                self.send_error(416)
                return
            start = int(match.group(1))
            end = int(match.group(2)) if match.group(2) else len(data) - 1
            if start >= len(data) or start > end:
                self.send_error(416)
                return
            end = min(end, len(data) - 1)
            body = data[start : end + 1]
            self.send_response(206)
            self.send_header("Content-Range", f"bytes {start}-{end}/{len(data)}")
        else:
            body = data
            self.send_response(200)
        self.send_header("Content-Type", "video/mp4")
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("ETag", self._etag())
        self.end_headers()
        for offset in range(0, len(body), 512):
            self.wfile.write(body[offset : offset + 512])
            self.wfile.flush()
            time.sleep(0.25)


def create_server(host: str = "127.0.0.1", port: int = 0) -> ThreadingHTTPServer:
    httpd = ThreadingHTTPServer((host, port), MediaRequestHandler)
    httpd.etag_value = "fixture-v1"
    httpd.disconnected_once = False
    httpd.slow_playable = False
    return httpd


if __name__ == "__main__":
    server = create_server("127.0.0.1", 8765)
    print("NovaFetch test media server: http://127.0.0.1:8765")
    server.serve_forever()
