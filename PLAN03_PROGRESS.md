# NovaFetch Plan 03 — Initial HLS parser checkpoint

Implemented an offline, bounded Kotlin HLS playlist parser supporting master variants, VOD segment URI resolution, relative paths, codec/bandwidth/resolution metadata, and encrypted-stream classification. Unsafe non-HTTP(S) segment URLs and malformed input are rejected.

Verification: `./tools/run-hls-parser-smoke.sh` passed in this container using `kotlinc` and Java.

Not yet implemented: HLS segment transfer/checkpointing, alternate audio rendition handling, DASH, mux/remux, Android runtime build/instrumentation. Encrypted media is classified as unsupported; no decryption or DRM bypass.

Prior Plan 01/02 Android runtime gates remain open.
