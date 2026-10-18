#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/smoke
kotlinc \
  media-detection/src/main/kotlin/app/novafetch/detection/MediaCandidate.kt \
  security/src/main/kotlin/app/novafetch/security/UrlPolicy.kt \
  browser-core/src/main/kotlin/app/novafetch/browser/ExtensionMessageValidator.kt \
  browser-core/src/test/kotlin/app/novafetch/browser/ExtensionMessageValidatorTest.kt \
  -include-runtime -d build/smoke/extension-validator.jar
java -jar build/smoke/extension-validator.jar
