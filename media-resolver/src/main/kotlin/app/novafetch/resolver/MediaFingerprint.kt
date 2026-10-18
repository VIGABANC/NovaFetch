package app.novafetch.resolver

object MediaFingerprint {
    // Do not remove query parameters: they may distinguish different media objects.
    fun of(item: ValidatedCandidate): String = "${item.candidate.kind}:${item.canonicalUrl}"
}
