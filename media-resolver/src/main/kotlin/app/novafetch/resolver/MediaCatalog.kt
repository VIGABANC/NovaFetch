package app.novafetch.resolver

class MediaCatalog(private val maxItems: Int = 100) {
    private val items = LinkedHashMap<String, ValidatedCandidate>()
    fun upsert(candidate: ValidatedCandidate): CatalogSnapshot {
        val key = MediaFingerprint.of(candidate)
        val existing = items[key]
        if (existing == null && items.size < maxItems || existing != null && candidate.confidence > existing.confidence)
            items[key] = candidate
        return snapshot()
    }
    fun snapshot(): CatalogSnapshot = CatalogSnapshot(items.values.sortedByDescending { it.confidence }
        .map { CatalogOption(MediaFingerprint.of(it), it) })
    fun clear() = items.clear()
}

data class CatalogOption(val id: String, val value: ValidatedCandidate) {
    val candidate get() = value.candidate
}
data class CatalogSnapshot(val options: List<CatalogOption>)
