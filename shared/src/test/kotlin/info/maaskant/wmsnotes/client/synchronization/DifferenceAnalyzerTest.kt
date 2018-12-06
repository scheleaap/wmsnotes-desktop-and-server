package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.AttachmentAddedEvent
import info.maaskant.wmsnotes.model.AttachmentDeletedEvent
import info.maaskant.wmsnotes.model.ContentChangedEvent
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.projection.Note
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*

internal class DifferenceAnalyzerTest {
    private val noteId = "note"
    private val title = "title"
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

    @Test
    fun existence() {
        // Given
        val left = Note()
        val right = left.apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = title)).first
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences).contains(
                ExistenceDifference(false, true)
        )
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
        assertThat(differences).isEqualTo(listOf(
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
        assertThat(differences).isEqualTo(listOf(
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
        assertThat(differences).isEqualTo(listOf(
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
        assertThat(differences).isEqualTo(listOf(
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
        assertThat(differences).isEqualTo(listOf(
                AttachmentDifference("att", attachmentContent, differentContent)
        ))
    }

    @Test
    fun `no differences`() {
        TODO("replace other tests with this one")
        val attachment1Name = UUID.randomUUID().toString()
        val attachment2Name = UUID.randomUUID().toString()
        val attachment3Name = UUID.randomUUID().toString()
        val differentContent = "different".toByteArray()
        val left = Note()
                .apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = title)).first
                .apply(AttachmentAddedEvent(eventId = 2, noteId = noteId, revision = 2, name = attachment1Name, content = attachmentContent)).first
        val right = left
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences).isEqualTo(emptySet())
    }

    @Test
    fun `all differences combined`() {
        TODO("replace other tests with this one")
        val attachment1Name = UUID.randomUUID().toString()
        val attachment2Name = UUID.randomUUID().toString()
        val attachment3Name = UUID.randomUUID().toString()
        val differentContent = "different".toByteArray()
        val left = Note()
                .apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = title)).first
                .apply(AttachmentAddedEvent(eventId = 2, noteId = noteId, revision = 2, name = attachment1Name, content = attachmentContent)).first
                .apply(AttachmentAddedEvent(eventId = 3, noteId = noteId, revision = 3, name = attachment3Name, content = attachmentContent)).first
        val right = Note()
                .apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = "different")).first
                .apply(ContentChangedEvent(eventId = 2, noteId = noteId, revision = 2, content = "different")).first
                .apply(AttachmentAddedEvent(eventId = 3, noteId = noteId, revision = 3, name = attachment2Name, content = attachmentContent)).first
                .apply(AttachmentDeletedEvent(eventId = 4, noteId = noteId, revision = 4, name = attachment3Name)).first
                .apply(AttachmentAddedEvent(eventId = 5, noteId = noteId, revision = 5, name = attachment3Name, content = differentContent)).first
        val analyzer = DifferenceAnalyzer()

        // When
        val differences = analyzer.compare(left, right)

        // Then
        assertThat(differences).isEqualTo(setOf(
                TitleDifference(title, "different"),
                ContentDifference("", "different"),
                AttachmentDifference(attachment1Name, attachmentContent, null),
                AttachmentDifference(attachment2Name, null, attachmentContent),
                AttachmentDifference(attachment3Name, attachmentContent, differentContent)
        ))
    }
}