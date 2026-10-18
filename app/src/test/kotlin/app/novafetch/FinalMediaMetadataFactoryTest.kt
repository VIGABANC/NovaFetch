package app.novafetch

import app.novafetch.download.DownloadPlan

fun main() {
    val factory = FinalMediaMetadataFactory()
    check(factory.from(DownloadPlan.DirectPlan("https://x/video", "video/mp4", "clip.mp4")).relativePath == "Movies/NovaFetch")
    check(factory.from(DownloadPlan.DirectPlan("https://x/audio", "audio/aac", "song.m4a")).relativePath == "Music/NovaFetch")
    check(factory.from(DownloadPlan.DirectPlan("https://x/image", "image/png", "cover.png")).relativePath == "Pictures/NovaFetch")
    check(factory.from(DownloadPlan.DirectPlan("https://x/file", "application/pdf", "doc.pdf")).relativePath == "Download/NovaFetch")
    check(factory.from(DownloadPlan.DirectPlan("https://x/unknown", null, "asset.bin")).mimeType == "application/octet-stream")
    println("FinalMediaMetadataFactoryTest: PASS")
}
