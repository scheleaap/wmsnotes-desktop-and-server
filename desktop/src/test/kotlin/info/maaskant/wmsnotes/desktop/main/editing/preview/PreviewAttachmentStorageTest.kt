package info.maaskant.wmsnotes.desktop.main.editing.preview

import info.maaskant.wmsnotes.desktop.main.editing.EditingViewModel
import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.utilities.Optional
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

internal class PreviewAttachmentStorageTest {
    private val note1Id = "note-1"
    private val note2Id = "note-2"
    private val path = Path("path")
    private val title = "Title"
    private val content = "Text"

    private lateinit var tempDir: File
    private val editingViewModel: EditingViewModel = mockk()
    private lateinit var state: PreviewAttachmentStorageState
    private val scheduler = Schedulers.trampoline()
    private lateinit var note: Subject<Optional<Note>>

    @BeforeEach
    fun init() {
        tempDir = createTempDir(this::class.simpleName!!).resolve("attachments")
        clearMocks(
                editingViewModel
        )
        note = PublishSubject.create()
        every { editingViewModel.getNote() }.returns(note)
        state = PreviewAttachmentStorageState()
    }

    @Test
    fun `no note`() {
        // Given
        val note1 = Optional<Note>()
        val storage = PreviewAttachmentStorage(tempDir, editingViewModel, initialState = state, scheduler = scheduler)
        val observer = storage.getAttachmentsStoredNotifications().test()

        // When
        note.onNext(note1)
        note.onNext(note1) // Twice

        // Then
        assertThat(observer.values()).isEqualTo(listOf(emptyMap<String, String>()))
    }

    @Test
    fun `initial note`() {
        // Given
        val note1 = createNote(note1Id, "att", "data")
        val storage = PreviewAttachmentStorage(tempDir, editingViewModel, initialState = state, scheduler = scheduler)
        val observer = storage.getAttachmentsStoredNotifications().test()

        // When
        note.onNext(note1)

        // Then
        assertThat(tempDir.resolve("att").exists()).isTrue()
        assertThat(tempDir.resolve("att").readBytes()).isEqualTo("data".toByteArray())
        assertThat(observer.values()).isEqualTo(listOf(emptyMap(), mapOf("att" to note1.value!!.attachmentHashes["att"]!!)))
    }

    @Test
    fun `initial note, no attachments`() {
        // Given
        val note1 = createNote(note1Id)
        val storage = PreviewAttachmentStorage(tempDir, editingViewModel, initialState = state, scheduler = scheduler)
        val observer = storage.getAttachmentsStoredNotifications().test()

        // When
        note.onNext(note1)

        // Then
        assertThat(observer.values()).isEqualTo(listOf<Map<String, String>>(emptyMap()))
    }

    @Test
    fun `update, attachments unchanged`() {
        // Given
        val note1v1 = createNote(note1Id, "att", "data")
        val note1v2 = Optional(note1v1.value!!
                .apply(ContentChangedEvent(eventId = 3, noteId = note1Id, revision = 3, content = "text")).component1()
        )
        val storage = PreviewAttachmentStorage(tempDir, editingViewModel, initialState = state, scheduler = scheduler)
        note.onNext(note1v1)
        val observer = storage.getAttachmentsStoredNotifications().test()

        // When
        note.onNext(note1v2)

        // Then
        assertThat(observer.values()).isEqualTo(listOf<Map<String, String>>(mapOf("att" to note1v2.value!!.attachmentHashes["att"]!!)))
    }

    @Test
    fun `update, attachments changed`() {
        // Given
        val note1v1 = createNote(note1Id, "att-1", "data1")
        val note1v2 = createNote(note1Id, "att-2", "data2")
        val storage = PreviewAttachmentStorage(tempDir, editingViewModel, initialState = state, scheduler = scheduler)
        note.onNext(note1v1)
        val observer = storage.getAttachmentsStoredNotifications().test()

        // When
        note.onNext(note1v2)

        // Then
        assertThat(observer.values()).isEqualTo(listOf(
                mapOf("att-1" to note1v1.value!!.attachmentHashes["att-1"]!!),
                mapOf("att-2" to note1v2.value!!.attachmentHashes["att-2"]!!)
        ))
    }

    @Test
    fun `do not store the same attachment twice`() {
        // Given
        val note1 = createNote(note1Id, "att", "data")
        val note2 = createNote(note2Id)
        PreviewAttachmentStorage(tempDir, editingViewModel, initialState = state, scheduler = scheduler)
        note.onNext(note1)
        note.onNext(note2)
        val file = tempDir.resolve("att")
        assertThat(file.delete()).isTrue()

        // When
        note.onNext(note1)

        // Then
        assertThat(file.exists()).isFalse()
    }

    private fun createNote(noteId: String): Optional<Note> =
            Optional(Note()
                    .apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, path = path, title = title, content = content)).component1()
            )

    private fun createNote(noteId: String, attachmentName: String, attachmentContent: String): Optional<Note> =
            Optional(Note()
                    .apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, path = path, title = title, content = content)).component1()
                    .apply(AttachmentAddedEvent(eventId = 2, noteId = noteId, revision = 2, name = attachmentName, content = attachmentContent.toByteArray())).component1())
}