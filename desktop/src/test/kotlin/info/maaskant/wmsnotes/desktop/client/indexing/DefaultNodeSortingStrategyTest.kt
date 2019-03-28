package info.maaskant.wmsnotes.desktop.client.indexing

import info.maaskant.wmsnotes.model.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class DefaultNodeSortingStrategyTest {
    private val aggId = "agg"
    private val path = Path()
    private val title = "Title"

    @Test
    fun `note - note, equal`() {
        // Given
        val note = Note(aggId = aggId, parentAggId = null, path = path, title = title)
        val strategy = DefaultNodeSortingStrategy()

        // When / then
        assertThat(strategy.compare(note, note)).isZero()
    }

    @Test
    fun `note - folder, sort by type`() {
        // Given
        val note = Note(aggId = aggId, parentAggId = null, path = path, title = "Title 1")
        val folder = Folder(aggId = aggId, parentAggId = null, path = path, title = "Title 2")
        val strategy = DefaultNodeSortingStrategy()

        // When / then
        assertThat(strategy.compare(note, folder)).isPositive()
        assertThat(strategy.compare(folder, note)).isNegative()
    }

    @Test
    fun `note - note, sort by title`() {
        // Given
        val note1 = Note(aggId = aggId, parentAggId = null, path = path, title = "Title 1")
        val note2 = Note(aggId = aggId, parentAggId = null, path = path, title = "Title 2")
        val strategy = DefaultNodeSortingStrategy()

        // When / then
        assertThat(strategy.compare(note1, note2)).isNegative()
        assertThat(strategy.compare(note2, note1)).isPositive()
    }

    @Test
    fun `folder - folder, sort by title`() {
        // Given
        val folder1 = Folder(aggId = aggId, parentAggId = null, path = path, title = "Title 1")
        val folder2 = Folder(aggId = aggId, parentAggId = null, path = path, title = "Title 2")
        val strategy = DefaultNodeSortingStrategy()

        // When / then
        assertThat(strategy.compare(folder1, folder2)).isNegative()
        assertThat(strategy.compare(folder2, folder1)).isPositive()
    }

    @Test
    fun `null values`() {
        // Given
        val note = Note(aggId = aggId, parentAggId = null, path = path, title = title)
        val strategy: Comparator<Node> = DefaultNodeSortingStrategy()

        // When / then
        assertThrows<IllegalArgumentException> { strategy.compare(note, null) }
        assertThrows<IllegalArgumentException> { strategy.compare(null, note) }
        assertThrows<IllegalArgumentException> { strategy.compare(null, null) }
    }

}