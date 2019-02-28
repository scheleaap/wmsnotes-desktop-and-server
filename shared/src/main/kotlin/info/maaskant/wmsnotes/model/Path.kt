package info.maaskant.wmsnotes.model

data class Path(val elements: List<String>) {
    constructor(vararg elements: String) : this(elements.toList())

    init {
        if (elements.isEmpty()) throw IllegalArgumentException("Empty path")
        elements
                .firstOrNull { it.isBlank() || it.contains('/') }
                ?.let {
                    throw IllegalArgumentException("Invalid path ($elements)")
                }
    }

    companion object {
        fun fromString(path: String): Path =
                Path(path.split('/'))
    }
}