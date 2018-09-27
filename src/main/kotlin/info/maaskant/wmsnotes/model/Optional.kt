package info.maaskant.wmsnotes.model

// Source: https://medium.com/@joshfein/handling-null-in-rxjava-2-0-10abd72afa0b
data class Optional<T>(val value: T?) {
    val isPresent = value != null
}
