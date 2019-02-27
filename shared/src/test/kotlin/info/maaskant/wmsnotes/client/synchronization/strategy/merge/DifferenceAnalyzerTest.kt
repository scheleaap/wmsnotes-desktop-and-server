package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.ExistenceDifference.ExistenceType.*
import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.projection.Note
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class DifferenceAnalyzerTest {
    private val noteId = "note"
    private val title = "title"
    private val text = "text"
    private val attachmentContent = "data".toByteArray()

    @Test
    fun `same empty notes`() {
        // Given
        val left = Note()
        val right = left
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences).isEmpty()
    }

    @TestFactory
    fun existence(): List<DynamicTest> {
        val newNote = Note()
        val createdNote = newNote.apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = title)).first
        val items = listOf(
                Triple(
                        newNote,
                        createdNote,
                        ExistenceDifference(NOT_YET_CREATED, EXISTS)
                ),
                Triple(
                        createdNote,
                        newNote,
                        ExistenceDifference(EXISTS, NOT_YET_CREATED)
                ),
                Triple(
                        createdNote,
                        createdNote.apply(NoteDeletedEvent(eventId = 2, noteId = noteId, revision = 2)).first,
                        ExistenceDifference(EXISTS, DELETED)
                ),
                Triple(
                        createdNote.apply(NoteDeletedEvent(eventId = 2, noteId = noteId, revision = 2)).first,
                        createdNote,
                        ExistenceDifference(DELETED, EXISTS)
                ),
                Triple(
                        newNote,
                        createdNote.apply(NoteDeletedEvent(eventId = 2, noteId = noteId, revision = 2)).first,
                        ExistenceDifference(NOT_YET_CREATED, DELETED)
                ),
                Triple(
                        createdNote.apply(NoteDeletedEvent(eventId = 2, noteId = noteId, revision = 2)).first,
                        newNote,
                        ExistenceDifference(DELETED, NOT_YET_CREATED)
                )
        )
        return items.map { (left, right, difference) ->
            DynamicTest.dynamicTest(difference.toString()) {
                // Given
                val analyzer = DifferenceAnalyzer()

                // When
                val differences = analyzer.compare(left, right)

                // Then
                assertThat(differences).contains(difference)
            }
        }
    }


    @Test
    fun title() {
        // Given
        val left = Note().apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = title)).first
        val right = Note().apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = "different")).first
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences).isEqualTo(setOf(
                TitleDifference(title, "different")
        ))
    }

    @Test
    fun content() {
        // Given
        val left = Note().apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = title)).first
        val right = left.apply(ContentChangedEvent(eventId = 2, noteId = noteId, revision = 2, content = "different")).first
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences).isEqualTo(setOf(
                ContentDifference("", "different")
        ))
    }

    @Test
    fun `attachment present`() {
        // Given
        val attachmentName = "att"
        val left = Note().apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = title)).first
        val right = left.apply(AttachmentAddedEvent(eventId = 2, noteId = noteId, revision = 2, name = attachmentName, content = attachmentContent)).first
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences).isEqualTo(setOf(
                AttachmentDifference(attachmentName, null, attachmentContent)
        ))
    }

    @Test
    fun `attachment not present`() {
        // Given
        val left = Note()
                .apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = title)).first
                .apply(AttachmentAddedEvent(eventId = 2, noteId = noteId, revision = 2, name = "att", content = attachmentContent)).first
        val right = left.apply(AttachmentDeletedEvent(eventId = 3, noteId = noteId, revision = 3, name = "att")).first
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences).isEqualTo(setOf(
                AttachmentDifference("att", attachmentContent, null)
        ))
    }

    @Test
    fun `attachment different`() {
        // Given
        val differentContent = "different".toByteArray()
        val left = Note()
                .apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = title)).first
                .apply(AttachmentAddedEvent(eventId = 2, noteId = noteId, revision = 2, name = "att", content = attachmentContent)).first
        val right = left
                .apply(AttachmentDeletedEvent(eventId = 3, noteId = noteId, revision = 3, name = "att")).first
                .apply(AttachmentAddedEvent(eventId = 4, noteId = noteId, revision = 4, name = "att", content = differentContent)).first
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences).isEqualTo(setOf(
                AttachmentDifference("att", attachmentContent, differentContent)
        ))
    }

    @Test
    fun `multiple differences`() {
        // Given
        val left = Note()
        val right = left
                .apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = title)).first
                .apply(AttachmentAddedEvent(eventId = 2, noteId = noteId, revision = 2, name = "att", content = attachmentContent)).first
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences.size).isGreaterThan(1)
    }

    @Test
    fun `real-world case 1`() {
        // Given
        val left = Note()
        val right = Note()
                .apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = title)).first
                .apply(ContentChangedEvent(eventId = 2, noteId = noteId, revision = 2, content = text)).first
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences).isEqualTo(setOf(
                ExistenceDifference(NOT_YET_CREATED, EXISTS),
                TitleDifference("", title),
                ContentDifference("", text)
        ))
    }

    @Test
    fun `real-world case 2`() {
        // Given
        val left = Note()
        val right = Note()
                .apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = title)).first
                .apply(ContentChangedEvent(eventId = 2, noteId = noteId, revision = 2, content = text)).first
                .apply(NoteDeletedEvent(eventId = 3, noteId = noteId, revision = 3)).first
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences).isEqualTo(setOf(
                ExistenceDifference(NOT_YET_CREATED, DELETED),
                TitleDifference("", title),
                ContentDifference("", text)
        ))
    }
}