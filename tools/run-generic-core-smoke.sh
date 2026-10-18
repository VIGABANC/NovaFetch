#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/smoke
CORE=(media-detection/src/main/kotlin/app/novafetch/detection/MediaCandidate.kt security/src/main/kotlin/app/novafetch/security/UrlPolicy.kt media-resolver/src/main/kotlin/app/novafetch/resolver/*.kt download-core/src/main/kotlin/app/novafetch/download/DownloadPlan.kt)
kotlinc "${CORE[@]}" media-resolver/src/test/kotlin/app/novafetch/resolver/MediaCatalogTest.kt -include-runtime -d build/smoke/media-catalog-test.jar
java -jar build/smoke/media-catalog-test.jar
kotlinc security/src/main/kotlin/app/novafetch/security/UrlPolicy.kt security/src/test/kotlin/app/novafetch/security/UrlPolicyTest.kt -include-runtime -d build/smoke/url-policy-test.jar
java -jar build/smoke/url-policy-test.jar
node --test tools/media-detector-tests/*.test.cjs
