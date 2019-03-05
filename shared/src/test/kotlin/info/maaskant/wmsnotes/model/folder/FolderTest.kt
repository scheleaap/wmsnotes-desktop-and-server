package info.maaskant.wmsnotes.model.folder

import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.note.Note
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows

@Suppress("RemoveRedundantBackticks")
internal class FolderTest {
    private val path = Path("el1", "el2")

    @TestFactory
    fun `wrong revision`(): List<DynamicTest> {
        return listOf(
                FolderDeletedEvent(eventId = 0, revision = 3, path = path)
                // Add more classes here
        ).map { event ->
            DynamicTest.dynamicTest(event::class.simpleName) {
                // Given
                val folder = folderWithEvents(FolderCreatedEvent(eventId = 0, revision = 1, path = path))

                // When / Then
                assertThat(event.revision).isGreaterThan(folder.revision + 1) // Validate the test data
                assertThrows<IllegalArgumentException> { folder.apply(event) }
            }
        }
    }

    @TestFactory
    fun `wrong path`(): List<DynamicTest> {
        return listOf(
                FolderCreatedEvent(eventId = 0, revision = 2, path = path),
                FolderDeletedEvent(eventId = 0, revision = 2, path = path)
                // Add more classes here
        ).map { event ->
            DynamicTest.dynamicTest(event::class.simpleName) {
                // Given
                val folder = folderWithEvents(FolderCreatedEvent(eventId = 0, revision = 1, path = Path("original")))

                // When / Then
                assertThat(event.path).isNotEqualTo(folder.path) // Validate the test data
                assertThrows<IllegalArgumentException> { folder.apply(event) }
            }
        }
    }

    @TestFactory
    fun `events that are not allowed as first event`(): List<DynamicTest> {
        return listOf(
                FolderDeletedEvent(eventId = 0, revision = 1, path = path)
                // Add more classes here
        ).map { event ->
            DynamicTest.dynamicTest(event::class.simpleName) {
                // Given
                val folder = Folder()

                // When / Then
                assertThat(event.revision).isEqualTo(1)
                assertThrows<IllegalArgumentException> { folder.apply(event) }
            }
        }
    }

    @Test
    fun `after instantiation`() {
        // When
        val folder = Folder()

        // Then
        assertThat(folder.revision).isEqualTo(0)
        assertThat(folder.exists).isEqualTo(false)
        assertThat(folder.path).isEqualTo(Path())
        assertThat(folder.title).isEmpty()
    }

    @Test
    fun `create first time`() {
        // Given
        val folderBefore = Folder()
        val eventIn = FolderCreatedEvent(eventId = 0, revision = 1, path = path)

        // When
        val (folderAfter, eventOut) = folderBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(folderBefore.revision).isEqualTo(0)
        assertThat(folderBefore.exists).isEqualTo(false)
        assertThat(folderAfter.revision).isEqualTo(eventIn.revision)
        assertThat(folderAfter.exists).isEqualTo(true)
        assertThat(folderAfter.aggId).isEqualTo(eventIn.aggId)
        assertThat(folderAfter.path).isEqualTo(eventIn.path)
        assertThat(folderAfter.title).isEqualTo(eventIn.path.elements.last())
    }

    @Test
    fun `create, empty path`() {
        // Given
        val folderBefore = Folder()
        val eventIn = FolderCreatedEvent(eventId = 0, revision = 1, path = Path())

        // When
        val (folderAfter, _) = folderBefore.apply(eventIn)

        // Then
        assertThat(folderAfter.aggId).isEqualTo(eventIn.aggId)
        assertThat(folderAfter.path).isEqualTo(eventIn.path)
        assertThat(folderAfter.title).isEmpty()
    }

    @Test
    fun `create first time, idempotence`() {
        // Given
        val folderBefore = folderWithEvents(FolderCreatedEvent(eventId = 0, revision = 1, path = path))
        val eventIn = FolderCreatedEvent(eventId = 0, revision = 2, path = path)

        // When
        val (folderAfter, eventOut) = folderBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(folderAfter).isEqualTo(folderBefore)
    }

    @Test
    fun `create after delete`() {
        // Given
        val folderBefore = folderWithEvents(
                FolderCreatedEvent(eventId = 0, revision = 1, path = path),
                FolderDeletedEvent(eventId = 0, revision = 2, path = path)
        )
        val eventIn = FolderCreatedEvent(eventId = 0, revision = 3, path = path)

        // When
        val (folderAfter, eventOut) = folderBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(folderBefore.revision).isEqualTo(2)
        assertThat(folderBefore.exists).isEqualTo(false)
        assertThat(folderAfter.revision).isEqualTo(3)
        assertThat(folderAfter.exists).isEqualTo(true)
    }

    @Test
    fun `create after delete, idempotence`() {
        // Given
        val folderBefore = folderWithEvents(
                FolderCreatedEvent(eventId = 0, revision = 1, path = path),
                FolderDeletedEvent(eventId = 0, revision = 2, path = path),
                FolderCreatedEvent(eventId = 0, revision = 3, path = path)
        )
        val eventIn = FolderCreatedEvent(eventId = 0, revision = 4, path = path)

        // When
        val (folderAfter, eventOut) = folderBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(folderAfter).isEqualTo(folderBefore)
    }

    @Test
    fun `delete`() {
        // Given
        val folderBefore = folderWithEvents(FolderCreatedEvent(eventId = 0, revision = 1, path = path))
        val eventIn = FolderDeletedEvent(eventId = 0, revision = 2, path = path)

        // When
        val (folderAfter, eventOut) = folderBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isEqualTo(eventIn)
        assertThat(folderBefore.revision).isEqualTo(1)
        assertThat(folderBefore.exists).isEqualTo(true)
        assertThat(folderAfter.revision).isEqualTo(2)
        assertThat(folderAfter.exists).isEqualTo(false)
    }

    @Test
    fun `delete, idempotence`() {
        // Given
        val folderBefore = folderWithEvents(
                FolderCreatedEvent(eventId = 0, revision = 1, path = path),
                FolderDeletedEvent(eventId = 0, revision = 2, path = path)
        )
        val eventIn = FolderDeletedEvent(eventId = 0, revision = 3, path = path)

        // When
        val (folderAfter, eventOut) = folderBefore.apply(eventIn)

        // Then
        assertThat(eventOut).isNull()
        assertThat(folderAfter).isEqualTo(folderBefore)
    }

    // Add more classes here

    @TestFactory
    fun `equals and hashCode for all fields`(): List<DynamicTest> {
        val revision = 0
        val exists = false
        val aggId = "folder"
        val path = Path("path")
        val original = Folder.deserialize(revision = revision, exists = exists, aggId = aggId, path = path)
        return listOf(
                "revision" to Folder.deserialize(revision = 1, exists = exists, aggId = aggId, path = path),
                "exists" to Folder.deserialize(revision = revision, exists = true, aggId = aggId, path = path),
                "aggId" to Folder.deserialize(revision = revision, exists = exists, aggId = "different", path = path),
                "path" to Folder.deserialize(revision = revision, exists = exists, aggId = aggId, path = Path("different"))
                // "" to Folder.deserialize(revision = revision,  exists = exists, aggId = aggId),
                // Add more fields here
        ).map {
            DynamicTest.dynamicTest(it.first) {
                // Given
                val modified = it.second

                // Then
                assertThat(original).isEqualTo(original)
                assertThat(original.hashCode()).isEqualTo(original.hashCode())
                assertThat(modified).isEqualTo(modified)
                assertThat(modified.hashCode()).isEqualTo(modified.hashCode())
                assertThat(original).isNotEqualTo(modified)
            }
        }
    }

    companion object {
        private fun folderWithEvents(vararg events: FolderEvent): Folder {
            var folder = Folder()
            for (event in events) {
                val (newFolder, _) = folder.apply(event)
                folder = newFolder
            }
            return folder
        }
    }
}
