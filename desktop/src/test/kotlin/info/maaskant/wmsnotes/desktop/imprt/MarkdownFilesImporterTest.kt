package info.maaskant.wmsnotes.desktop.imprt

import info.maaskant.wmsnotes.model.ChangeContentCommand
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.CreateNoteCommand
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

internal class MarkdownFilesImporterTest {
    private val noteId = "note"

    private lateinit var tempDir: File
    private var commandProcessor: CommandProcessor = mockk()

    @BeforeEach
    fun init() {
        tempDir = createTempDir(this::class.simpleName!!).resolve("files")
        clearMocks(
                commandProcessor
        )
        every { commandProcessor.blockingProcessCommand(CreateNoteCommand(noteId = null, title = any())) }.returns(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = ""))
        every { commandProcessor.commands.onNext(any()) }.just(Runs)
    }

    @Test
    fun `empty file`() {
        // Given
        createMd(filename = "Title.md")
        val importer = createInstance()

        // When
        importer.import(tempDir)

        // Then
        verifySequence {
            commandProcessor.blockingProcessCommand(CreateNoteCommand(noteId = null, title = "Title"))
        }
    }

    @Test
    fun `file with content, no header`() {
        // Given
        val content = "Data"
        createMd(filename = "Title.md", content = content)
        val importer = createInstance()

        // When
        importer.import(tempDir)

        // Then
        verifySequence {
            commandProcessor.blockingProcessCommand(CreateNoteCommand(noteId = null, title = "Title"))
            commandProcessor.commands.onNext(ChangeContentCommand(noteId = noteId, lastRevision = 1, content = content))
        }
    }

    @Test
    fun `file with content, header different from file name`() {
        // Given
        val content = "# Different\nData"
        createMd(filename = "Title.md", content = content)
        val importer = createInstance()

        // When
        importer.import(tempDir)

        // Then
        verifySequence {
            commandProcessor.blockingProcessCommand(CreateNoteCommand(noteId = null, title = "Title"))
            commandProcessor.commands.onNext(ChangeContentCommand(noteId = noteId, lastRevision = 1, content = content))
        }
    }

    @Test
    fun `file with content, header equal to file name`() {
        // Given
        val fileContent = "# Title\nData"
        val importedContent = "Data"
        createMd(filename = "Title.md", content = fileContent)
        val importer = createInstance()

        // When
        importer.import(tempDir)

        // Then
        verifySequence {
            commandProcessor.blockingProcessCommand(CreateNoteCommand(noteId = null, title = "Title"))
            commandProcessor.commands.onNext(ChangeContentCommand(noteId = noteId, lastRevision = 1, content = importedContent))
        }
    }
    @Test
    fun `convert line endings`() {
        // Given
        val fileContent = "Data\r\nBla\n"
        val importedContent = "Data\nBla"
        createMd(filename = "Title.md", content = fileContent)
        val importer = createInstance()

        // When
        importer.import(tempDir)

        // Then
        verifySequence {
            commandProcessor.blockingProcessCommand(CreateNoteCommand(noteId = null, title = "Title"))
            commandProcessor.commands.onNext(ChangeContentCommand(noteId = noteId, lastRevision = 1, content = importedContent))
        }
    }

    private fun createMd(filename: String, content: String = "") {
        tempDir.resolve(filename).writeText(content)
    }


    private fun createInstance(): MarkdownFilesImporter {
        return MarkdownFilesImporter(commandProcessor)
    }
}