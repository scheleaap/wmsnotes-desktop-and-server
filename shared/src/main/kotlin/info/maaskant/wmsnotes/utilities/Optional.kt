package info.maaskant.wmsnotes.utilities

// Source: https://medium.com/@joshfein/handling-null-in-rxjava-2-0-10abd72afa0b
data class Optional<T>(val value: T?) {
    val isPresent = value != null

    constructor() : this(null)

    fun <R> map(function: (T) -> R): Optional<R> {
        return if (value != null) {
            Optional(function(value))
        } else {
            Optional()
        }
    }
}

//fun <T> T.toOptional(): Optional<T> {
//    return Optional(this)
//}
