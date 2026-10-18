#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
kotlinc media-resolver/src/main/kotlin/app/novafetch/resolver/HlsPlaylistParser.kt media-resolver/src/test/kotlin/app/novafetch/resolver/HlsPlaylistParserPortableTest.kt -include-runtime -d /tmp/novafetch-hls-parser-test.jar
java -jar /tmp/novafetch-hls-parser-test.jar
