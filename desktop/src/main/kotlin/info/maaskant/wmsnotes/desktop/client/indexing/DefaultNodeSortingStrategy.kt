package info.maaskant.wmsnotes.desktop.client.indexing

class DefaultNodeSortingStrategy : Comparator<Node> {
    override fun compare(o1: Node, o2: Node): Int =
            compareValuesBy(o1, o2, { getValueForClass(it) }, { it.title })

    private fun getValueForClass(node: Node): Int =
            when (node) {
                is Folder -> 0
                is Note -> 1
            }
}
