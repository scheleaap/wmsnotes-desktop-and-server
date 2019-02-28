package info.maaskant.wmsnotes.model

data class Path(val elements: List<String>) {
    constructor(vararg elements: String) : this(elements.toList())

    init {
        if (!elements.isEmpty()) {
            elements
                    .firstOrNull { it.isBlank() || it.contains('/') }
                    ?.let {
                        throw IllegalArgumentException("Invalid path ($elements)")
                    }
        }
    }

    override fun toString(): String =
            elements.joinToString(separator = "/")

    companion object {
        fun from(path: String): Path =
                if (path.isEmpty()) {
                    Path()
                } else {
                    Path(path.split('/'))
                }
    }
}