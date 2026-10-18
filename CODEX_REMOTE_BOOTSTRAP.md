# Codex remote bootstrap — NovaFetch

**Official repository:** https://github.com/VIGABANC/NovaFetch

> Run the following in a Codex environment with Git, JDK, Node.js, Python 3 and Android SDK support. This is an unfinished Android project, not an installable APK release yet.

## Master instruction to Codex

You are a senior Android/Kotlin engineer and test-driven delivery agent. Continue the EXISTING NovaFetch project. Do not recreate it from scratch.

1. Clone the public source repository:

```bash
git clone https://github.com/VIGABANC/NovaFetch.git
cd NovaFetch
git status --short
git switch -c codex/android-build-foundation
```

2. Read `AGENTS.md`, `CODEX_CONTINUATION_PROMPT.md`, `README.md`, the three status/progress files, `docs/implementation/STATUS.md`, the Superpowers spec/plans, all Gradle module configurations, Android manifests, and existing source/tests. Follow the most recent code and tests as source of truth.
3. Establish a REAL baseline. Run the local suites (below) and record stdout, exit codes and failures in `docs/implementation/STATUS.md`. Do NOT claim Android builds or device behavior unless the required tasks actually ran successfully.
4. Fix Android build prerequisites first: compatible JDK, Android SDK, Gradle, Android Gradle Plugin, Kotlin, Room/KSP, Compose, GeckoView and Media3. The current repository has `gradle/wrapper/gradle-wrapper.properties` but not yet a complete Gradle wrapper; restore official Gradle wrapper scripts/JAR via a trusted versioned distribution or available Gradle installation, and commit reproducible build prerequisites. Never disable tests to hide compile errors.
5. Run `bash tools/verify-android-build.sh`, `./gradlew :app:assembleDebug`, relevant unit tests and `connectedDebugAndroidTest` on Android emulator/device when available. If no SDK/emulator exists, record a precise blocker and work on independently testable modules. Never invent results.
6. Demonstrate the entire direct-file E2E slice on a device using the fixture server: GeckoView page → validated media detection → choose MP4 → enqueue → pause → resume → safe Range/ETag handling → persisted Room state → process/network recovery → MediaStore finalization → Library → offline Media3 playback. Fix bugs test-first and commit small reviewable changes.
7. Then implement HLS VOD segment transfers/checkpoints and handling of non-DRM quality variants; DASH VOD representation parsing/transfer; separate audio-video mux/remux; safe limited site adapters; browser privacy and polished offline player. Work from `CODEX_CONTINUATION_PROMPT.md` for detailed requirements and the existing Superpowers spec/plans. Do not defeat DRM, paywalls, authentication or rights restrictions.
8. Ensure clean developer documentation, Android tests, CI build/verification gate, and versioned APK packaging. Do NOT commit signing keys or credentials.
9. Work continuously across tasks where tools permit, reporting completed tests/commits and exact blockers. Do not request repeated permission for small reversible changes. Do not push to `main` or merge without checking all gates and user authorization.

### First local commands

```bash
node --test tools/media-detector-tests/*.test.cjs
python3 -m unittest discover -s tools/test-media-server -p 'test*.py' -q
bash tools/run-hls-parser-smoke.sh
bash tools/verify-android-build.sh
```

### Completion gate

A successful source parse or Node/Kotlin smoke test is NOT proof of a buildable APK. Provide the actual APK path and build logs only after Gradle and required Android checks pass. Label incomplete HLS/DASH/muxing appropriately. Continue implementing and validating without inventing functionality.
