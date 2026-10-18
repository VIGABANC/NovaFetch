package app.novafetch.security

fun main() {
    listOf("https://video.example/a.mp4", "http://127.0.0.1:8123/sample.mp4")
        .forEach { check(UrlPolicy.allowRemoteMedia(it)) }
    listOf("blob:https://example.com/a", "javascript:alert(1)", "file:///private", "content://secret", "http://admin:secret@example.org/video.mp4", "https://")
        .forEach { check(!UrlPolicy.allowRemoteMedia(it)) { "must reject $it" } }
    check(UrlPolicy.canonicalize("https://cdn.example/file.mp4#part") == "https://cdn.example/file.mp4")
    val signed = "https://cdn.example/video%20name.mp4?token=a%2Bb%2Fc&expires=123#preview"
    check(UrlPolicy.canonicalize(signed) == signed.substringBefore('#')) { "signed URL bytes must be preserved" }
    println("UrlPolicyTest: PASS")
}
