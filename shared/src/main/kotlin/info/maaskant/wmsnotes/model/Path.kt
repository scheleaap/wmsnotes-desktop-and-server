package info.maaskant.wmsnotes.model

data class Path(val elements: List<String>) {
    val isRoot: Boolean = elements.isEmpty()

    constructor(vararg elements: String) : this(elements.toList())

    init {
        if (elements.isNotEmpty()) {
            elements
                    .firstOrNull { it.isBlank() || it.contains('/') }
                    ?.let {
                        throw IllegalArgumentException("Invalid path ($elements)")
                    }
        }
    }

    fun child(childElement: String): Path {
        return Path(elements + childElement)
    }

    fun parent(): Path =
            if (elements.isNotEmpty()) {
                Path(elements.subList(0, elements.lastIndex))
            } else {
                throw IllegalStateException("Root path does not have a parent")
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