# Codex / Agent Instructions — NovaFetch

**Read `CODEX_CONTINUATION_PROMPT.md` first.** Then read `README.md`, `NATIVE_IMPLEMENTATION_STATUS.md`, `PLAN02_SOURCE_STATUS.md`, `PLAN03_PROGRESS.md`, and the actual files under `docs/superpowers/`.

- Work with the existing Android/Kotlin project. Do not recreate from scratch.
- Primary goal: **verified Android build + on-device direct detection/download/playback**, then HLS VOD/DASH VOD and resilience.
- Use short TDD loops and create one reviewable commit per implemented unit. Verify tests before claiming completion.
- Expected modules: app, common, data, download-core, download-android, library, player, security, browser-core, browser-ui, media-detection, media-resolver.
- `minSdk 29`; check the current pinned versions in `gradle/libs.versions.toml` for actual compatibility before modifying them.
- `tools/verify-android-build.sh` and `tools/verify-android-runtime.sh` define pending Android gates. No current snapshot demonstrates a built/released APK.
- Portable smoke commands include `node --test tools/media-detector-tests/*.test.cjs`, `python3 -m unittest discover -s tools/test-media-server -p 'test*.py' -q`, and `bash tools/run-hls-parser-smoke.sh`.
- Download only user-authorized content. No DRM/access-control/paywall bypass, token extraction, remote executable site adapters or proprietary app copying.
- Protect credentials and session data. No `MANAGE_EXTERNAL_STORAGE`. Room for jobs, private temp files, MediaStore for finalized media.
- GitHub target: `VIGABANC/NovaFetch`. Last verified public and empty; original user preference Private. Do not push publicly unless explicitly authorized. Keep history and use feature branch + PR.
- Update `docs/implementation/STATUS.md` with actual verified results and blockers after milestones. Do not claim that tests ran if they did not.
