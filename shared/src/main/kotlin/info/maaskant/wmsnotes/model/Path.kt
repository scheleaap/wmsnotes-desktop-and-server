package info.maaskant.wmsnotes.model

data class Path(val elements: List<String>) {
    constructor(vararg elements: String) : this(elements.toList())

    companion object {
        fun fromString(path: String): Path =
                Path(path.split('/'))
    }
}