package kotlinx.coroutines.flow

interface Flow<out T>

@Suppress("UNUSED_PARAMETER")
fun <T, R> Flow<T>.map(transform: (T) -> R): Flow<R> = object : Flow<R> {}
