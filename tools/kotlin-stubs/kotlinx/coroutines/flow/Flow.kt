package kotlinx.coroutines.flow

interface Flow<out T>

fun <T> emptyFlow(): Flow<T> = object : Flow<T> {}

@Suppress("UNUSED_PARAMETER")
fun <T, R> Flow<T>.map(transform: (T) -> R): Flow<R> = object : Flow<R> {}
