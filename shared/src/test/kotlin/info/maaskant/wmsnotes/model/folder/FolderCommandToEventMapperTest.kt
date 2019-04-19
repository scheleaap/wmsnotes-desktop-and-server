package info.maaskant.wmsnotes.model.folder

import info.maaskant.wmsnotes.model.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal class FolderCommandToEventMapperTest {
    private val path = Path("el1", "el2")

    @TestFactory
    fun test(): List<DynamicTest> {
        val lastRevision1 = 11
        val lastRevision2 = 15
        val eventRevision1 = lastRevision1 + 1
        val eventRevision2 = lastRevision2 + 1

        val pairs = listOf(
                CreateFolderCommand(path = path) to FolderCreatedEvent(eventId = 0, revision = eventRevision2, path = path),
                DeleteFolderCommand(path = path, lastRevision = lastRevision1) to FolderDeletedEvent(eventId = 0, revision = eventRevision1, path = path)
                // Add more classes here
        )
        return pairs.map { (command, expectedEvent) ->
            DynamicTest.dynamicTest("${command::class.simpleName} to ${expectedEvent::class.simpleName}") {
                assertThat(FolderCommandToEventMapper().map(command, lastRevision2)).isEqualTo(expectedEvent)
            }
        }
    }
}