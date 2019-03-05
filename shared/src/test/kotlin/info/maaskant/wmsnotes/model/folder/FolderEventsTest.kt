package info.maaskant.wmsnotes.model.folder

import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderDeletedEvent
import info.maaskant.wmsnotes.model.note.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class FolderEventsTest {
    private val path = Path("el1", "el2")
    private val hash = "dbd167170df1ac9647c2c0d750ceb34e41a63741"

    @TestFactory
    fun `aggregate id`(): List<DynamicTest> {
        return listOf(
                FolderCreatedEvent(eventId = 0, revision = 1, path = path),
                FolderDeletedEvent(eventId = 0, revision = 1, path = path)
                // Add more classes here
        ).map {
            DynamicTest.dynamicTest(it::class.simpleName) {
                assertThat(it.aggId).isEqualTo(hash)
            }
        }
    }
}
