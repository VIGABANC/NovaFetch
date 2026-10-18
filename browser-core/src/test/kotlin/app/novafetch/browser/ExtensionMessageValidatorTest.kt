package app.novafetch.browser

fun main() {
    val validator = ExtensionMessageValidator()
    val sender = ExtensionSenderContext("media-detector@novafetch.app", true, "https://example.org/watch")
    val valid = DomMediaMessage(1, "media_candidates", "https://example.org/watch", listOf(
        DomMediaHint("https://cdn.example.org/movie.mp4", "video", "dom", "video/mp4")
    ))
    val result = validator.validate(sender, valid)
    check(result.isSuccess && result.getOrThrow().size == 1)
    check(validator.validate(sender.copy(extensionId = "stranger@evil.org"), valid).isFailure)
    check(validator.validate(sender.copy(sessionMatches = false), valid).isFailure)
    check(validator.validate(sender.copy(frameUrl = "https://evil.example/"), valid).isFailure)
    check(validator.validate(sender, valid.copy(schemaVersion = 999)).isFailure)
    check(validator.validate(sender, valid.copy(items = listOf(DomMediaHint("file:///private", "video", "dom", "video/mp4")))).isFailure)
    check(validator.validate(sender, valid.copy(items = List(11) { valid.items.first() })).isFailure)
    check(validator.validate(sender, valid.copy(items = listOf(DomMediaHint("https://cdn.example.org/f.mp4", "video", "unknown", "video/mp4")))).isFailure)
    println("ExtensionMessageValidatorTest: PASS")
}
