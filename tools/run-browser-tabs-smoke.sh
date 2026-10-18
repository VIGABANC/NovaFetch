#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/smoke
kotlinc browser-core/src/main/kotlin/app/novafetch/browser/BrowserTabRegistry.kt browser-core/src/test/kotlin/app/novafetch/browser/BrowserTabRegistryTest.kt -include-runtime -d build/smoke/tab-registry-test.jar
java -jar build/smoke/tab-registry-test.jar
