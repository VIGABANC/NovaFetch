package app.novafetch.download

sealed interface RetryDecision {
    data class RetryAfter(val delayMs: Long) : RetryDecision
    data object Fail : RetryDecision
}

class RetryPolicy(
    private val maxRetries: Int,
    private val baseDelayMs: Long,
) {
    init {
        require(maxRetries >= 0) { "maxRetries cannot be negative" }
        require(baseDelayMs > 0) { "baseDelayMs must be positive" }
    }

    fun forHttpStatus(statusCode: Int, retryCount: Int): RetryDecision {
        if (retryCount >= maxRetries) return RetryDecision.Fail
        if (statusCode !in setOf(502, 503, 504)) return RetryDecision.Fail
        val multiplier = 1L shl retryCount.coerceIn(0, 30)
        return RetryDecision.RetryAfter(baseDelayMs * multiplier)
    }
}
