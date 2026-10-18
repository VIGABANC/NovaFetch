package app.novafetch.browser

fun main() {
    val store = BrowserTabRegistry()
    val first = store.open("https://example.org")
    val second = store.open("https://mozilla.org")
    check(store.list().size == 2)
    check(store.activeId() == second)
    store.activate(first)
    check(store.activeId() == first)
    store.navigate(first, "https://example.org/video")
    check(store.list().first { it.id == first }.url.endsWith("/video"))
    store.close(first)
    check(store.activeId() == second && store.list().size == 1)
    store.close(second)
    check(store.activeId() == null)
    println("BrowserTabRegistryTest: PASS")
}
