# NovaFetch deterministic test media server

Run:

```bash
python3 server.py
```

Default address: `http://127.0.0.1:8765`.

Current deterministic endpoints:

- `/direct.mp4` — full body, no advertised byte ranges.
- `/range.mp4` — supports `Range`, ETag/If-Range, and returns `206`.
- `/no-range.mp4` — ignores `Range` and returns the full body.
- `/redirect.mp4` — redirects to `/direct.mp4`.
- `/error-503` — deterministic retryable server error.
- `/disconnect-mid-download` — declares the full length then closes after half.
- `/expired-url` — `403` unless `?token=fresh` is present.
- `/control/etag?value=...` — changes the fixture ETag for source-change tests.

The `direct.mp4` fixture is deterministic transfer data, not a valid encoded movie. Media-container fixtures are added in the media-processing plan.
