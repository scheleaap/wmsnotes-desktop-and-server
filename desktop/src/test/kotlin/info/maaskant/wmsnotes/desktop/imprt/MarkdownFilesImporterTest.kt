package info.maaskant.wmsnotes.desktop.imprt

import info.maaskant.wmsnotes.model.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime

@Disabled("Tests written while traveling, code to be implemented next")
internal class MarkdownFilesImporterTest {
    private val noteId = "note"
    private val clock: Clock = Clock.fixed(ZonedDateTime.of(2001, 2, 3, 4, 5, 6, 7, ZoneId.of("UTC")).toInstant())
    private val importFolderName: String = "Import of 2001-02-03 04:05:06"

    private lateinit var tempDir: File
    private var commandProcessor: CommandProcessor = mockk()

    @BeforeEach
    fun init() {
        tempDir = createTempDir(this::class.simpleName!!).resolve("files")
        clearMocks(
                commandProcessor
        )
        every { commandProcessor.blockingProcessCommand(CreateNoteCommand(noteId = null, path = any(), title = any(), content = any())) }.returns(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, path = Path("el"), title = "", content = ""))
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
        verify {
            commandProcessor.commands.onNext(CreateNoteCommand(noteId = null, path = Path(importFolderName), title = "Title", content = ""))
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
        verify {
            commandProcessor.commands.onNext(CreateNoteCommand(noteId = null, path = Path(importFolderName), title = "Title", content = content))
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
        verify {
            commandProcessor.commands.onNext(CreateNoteCommand(noteId = null, path = Path(importFolderName), title = "Title", content = content))
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
        verify {
            commandProcessor.commands.onNext(CreateNoteCommand(noteId = null, path = Path(importFolderName), title = "Title", content = importedContent))
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
        verify {
            commandProcessor.commands.onNext(CreateNoteCommand(noteId = any(), path = any(), title = any(), content = importedContent))
        }
    }

    @Test
    fun `folders`() {
        // Given
        createMd(path = "el1/el2", filename = "Title.md")
        val importer = createInstance()

        // When
        importer.import(tempDir)

        // Then
        verifySequence {
            commandProcessor.commands.onNext(CreateFolderCommand(aggregateId = importFolderName, path = Path(importFolderName), title = importFolderName)),
            commandProcessor.commands.onNext(CreateFolderCommand(aggregateId = "$importFolderName/el1", path = Path(importFolderName, "el1"), title = "el1")),
            commandProcessor.commands.onNext(CreateFolderCommand(aggregateId = "$importFolderName/el1/el2", path = Path(importFolderName, "el1", "el2"), title = "el2")),
            commandProcessor.commands.onNext(CreateNoteCommand(noteId = null, path = Path(importFolderName, "el1", "el2"), title = any(), content = any()))
        }
    }

    private fun createMd(filename: String, content: String = "", path: String = "") {
        val dir = if (path == "") {
            tempDir
        } else {
            tempDir.resolve(path)
        }
        dir.resolve(filename).writeText(content)
    }


    private fun createInstance(): MarkdownFilesImporter {
        return MarkdownFilesImporter(commandProcessor, clock)
    }
}