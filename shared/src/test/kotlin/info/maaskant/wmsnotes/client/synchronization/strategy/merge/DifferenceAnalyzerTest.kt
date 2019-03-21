package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.ExistenceDifference.ExistenceType.*
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.note.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class DifferenceAnalyzerTest {
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
        val createdNote = newNote.apply(noteCreatedEvent()).first
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
                        createdNote.apply(NoteDeletedEvent(eventId = 2, aggId = aggId, revision = 2)).first,
                        ExistenceDifference(EXISTS, DELETED)
                ),
                Triple(
                        createdNote.apply(NoteDeletedEvent(eventId = 2, aggId = aggId, revision = 2)).first,
                        createdNote,
                        ExistenceDifference(DELETED, EXISTS)
                ),
                Triple(
                        newNote,
                        createdNote.apply(NoteDeletedEvent(eventId = 2, aggId = aggId, revision = 2)).first,
                        ExistenceDifference(NOT_YET_CREATED, DELETED)
                ),
                Triple(
                        createdNote.apply(NoteDeletedEvent(eventId = 2, aggId = aggId, revision = 2)).first,
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
    fun path() {
        // Given
        val left = Note().apply(noteCreatedEvent(path = path)).first
        val right = left.apply(MovedEvent(eventId = 2, aggId = aggId, revision = 2, path = Path("different"))).first
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences).isEqualTo(setOf(
                PathDifference(path, Path("different"))
        ))
    }

    @Test
    fun title() {
        // Given
        val left = Note().apply(noteCreatedEvent(title = title)).first
        val right = Note().apply(noteCreatedEvent(title = "different")).first
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
        val left = Note().apply(noteCreatedEvent(content = content)).first
        val right = left.apply(ContentChangedEvent(eventId = 2, aggId = aggId, revision = 2, content = "different")).first
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences).isEqualTo(setOf(
                ContentDifference(content, "different")
        ))
    }

    @Test
    fun `attachment present`() {
        // Given
        val attachmentName = "att"
        val left = Note().apply(noteCreatedEvent()).first
        val right = left.apply(AttachmentAddedEvent(eventId = 2, aggId = aggId, revision = 2, name = attachmentName, content = attachmentContent)).first
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
                .apply(noteCreatedEvent()).first
                .apply(AttachmentAddedEvent(eventId = 2, aggId = aggId, revision = 2, name = "att", content = attachmentContent)).first
        val right = left.apply(AttachmentDeletedEvent(eventId = 3, aggId = aggId, revision = 3, name = "att")).first
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
                .apply(noteCreatedEvent()).first
                .apply(AttachmentAddedEvent(eventId = 2, aggId = aggId, revision = 2, name = "att", content = attachmentContent)).first
        val right = left
                .apply(AttachmentDeletedEvent(eventId = 3, aggId = aggId, revision = 3, name = "att")).first
                .apply(AttachmentAddedEvent(eventId = 4, aggId = aggId, revision = 4, name = "att", content = differentContent)).first
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
                .apply(noteCreatedEvent()).first
                .apply(AttachmentAddedEvent(eventId = 2, aggId = aggId, revision = 2, name = "att", content = attachmentContent)).first
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
                .apply(noteCreatedEvent(eventId = 1, aggId = aggId, revision = 1, path = Path(), title = title, content = "")).first
                .apply(ContentChangedEvent(eventId = 2, aggId = aggId, revision = 2, content = content)).first
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences).isEqualTo(setOf(
                ExistenceDifference(NOT_YET_CREATED, EXISTS),
                TitleDifference("", title),
                ContentDifference("", content)
        ))
    }

    @Test
    fun `real-world case 2`() {
        // Given
        val left = Note()
        val right = Note()
                .apply(NoteCreatedEvent(eventId = 1, aggId = aggId, revision = 1, path = Path(), title = title, content = "")).first
                .apply(ContentChangedEvent(eventId = 2, aggId = aggId, revision = 2, content = content)).first
                .apply(NoteDeletedEvent(eventId = 3, aggId = aggId, revision = 3)).first
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences).isEqualTo(setOf(
                ExistenceDifference(NOT_YET_CREATED, DELETED),
                TitleDifference("", title),
                ContentDifference("", content)
        ))
    }

    companion object {
        private const val aggId = "note"
        private val path = Path("path")
        private const val title = "title"
        private const val content = "content"
        private val attachmentContent = "data".toByteArray()

        fun noteCreatedEvent(
                eventId: Int = 1,
                aggId: String = DifferenceAnalyzerTest.aggId,
                revision: Int = 1,
                path: Path = DifferenceAnalyzerTest.path,
                title: String = DifferenceAnalyzerTest.title,
                content: String = DifferenceAnalyzerTest.content
        ): NoteCreatedEvent =
                NoteCreatedEvent(
                        eventId = eventId,
                        aggId = aggId,
                        revision = revision,
                        path = path,
                        title = title,
                        content = content
                )
    }
}