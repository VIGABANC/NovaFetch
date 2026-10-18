package app.novafetch.resolver

fun main() {
    val parser = HlsPlaylistParser()
    val master = parser.parse("""
        #EXTM3U
        #EXT-X-STREAM-INF:BANDWIDTH=1500000,RESOLUTION=1280x720,CODECS="avc1.42e01e,mp4a.40.2"
        720/index.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=3200000,RESOLUTION=1920x1080
        1080/index.m3u8
    """.trimIndent(), "https://media.example/master.m3u8")
    check(master.variants.size == 2)
    check(master.variants[0].codecs == "avc1.42e01e,mp4a.40.2")
    check(master.variants[1].uri == "https://media.example/1080/index.m3u8")
    val vod = parser.parse("""
        #EXTM3U
        #EXT-X-TARGETDURATION:10
        #EXTINF:10,
        seg01.ts
        #EXTINF:5,
        seg02.ts
        #EXT-X-ENDLIST
    """.trimIndent(), "https://media.example/playlist/index.m3u8")
    check(vod.segments.size == 2 && vod.endList && !vod.protected)
    val encrypted = parser.parse("#EXTM3U\n#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\"\n#EXTINF:5,\na.ts\n#EXT-X-ENDLIST", "https://media.example/vod.m3u8")
    check(encrypted.protected)
    check(runCatching { parser.parse("garbage", "https://media.example/a.m3u8") }.isFailure)
    check(runCatching { parser.parse("#EXTM3U\nfile:///etc/passwd", "https://media.example/a.m3u8") }.isFailure)
    println("HLS parser portable tests PASS")
}
