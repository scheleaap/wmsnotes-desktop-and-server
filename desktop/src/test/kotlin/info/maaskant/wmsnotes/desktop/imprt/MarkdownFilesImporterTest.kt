package info.maaskant.wmsnotes.desktop.imprt

import info.maaskant.wmsnotes.desktop.imprt.MarkdownFilesImporter.*
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.note.CreateNoteCommand
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime

internal class MarkdownFilesImporterTest {
    private val zone = ZoneId.of("UTC")
    private val clock: Clock = Clock.fixed(ZonedDateTime.of(2001, 2, 3, 4, 5, 6, 7, zone).toInstant(), zone)
    private val importFolderName: String = "Import of 2001-02-03 04:05:06"
    private val basePath = Path("a", "b")

    private lateinit var tempDir: File
    private var commandProcessor: CommandProcessor = mockk()

    @BeforeEach
    fun init() {
        tempDir = createTempDir(this::class.simpleName!!).resolve("files")
        clearMocks(
                commandProcessor
        )
//        every { commandProcessor.blockingProcessCommand(CreateNoteCommand(aggId = null, path = any(), title = any(), content = any())) }.returns(NoteCreatedEvent(eventId = 1, aggId = aggId, revision = 1, path = Path("el"), title = "", content = ""))
        every { commandProcessor.commands.onNext(any()) }.just(Runs)
    }

    @Test
    fun `load, empty file`() {
        // Given
        createFile(filename = "Title.md")
        val importer = createInstance()

        // When
        val nodes = importer.load(rootDirectory = tempDir, basePath = basePath).toList().blockingGet()

        // Then
        assertThat(nodes).hasSize(2)
        assertThat(nodes.toSet()).isEqualTo(setOf(
                ImportableNode.Folder(path = basePath),
                ImportableNode.Note(path = basePath, title = "Title", content = "")
        ))
    }

    @Test
    fun `load, file with content`() {
        // Given
        val content = "Content"
        createFile(filename = "Title.md", content = content)
        val importer = createInstance()

        // When
        val nodes = importer.load(rootDirectory = tempDir, basePath = basePath).toList().blockingGet()

        // Then
        assertThat(nodes).hasSize(2)
        assertThat(nodes.toSet()).isEqualTo(setOf(
                ImportableNode.Folder(path = basePath),
                ImportableNode.Note(path = basePath, title = "Title", content = content)
        ))
    }

    @Test
    fun `load, multiple files with content`() {
        // Given
        createFile(filename = "Title 1.md", content = "Content 1")
        createFile(filename = "Title 2.md", content = "Content 2")
        val importer = createInstance()

        // When
        val nodes = importer.load(rootDirectory = tempDir, basePath = basePath).toList().blockingGet()

        // Then
        assertThat(nodes).hasSize(3)
        assertThat(nodes.toSet()).isEqualTo(setOf(
                ImportableNode.Folder(path = basePath),
                ImportableNode.Note(path = basePath, title = "Title 1", content = "Content 1"),
                ImportableNode.Note(path = basePath, title = "Title 2", content = "Content 2")
        ))
    }

    @Test
    fun `load, non-markdown files`() {
        // Given
        createFile(filename = "file.txt")
        createFile(filename = "file.jpg", path = "foo/bar")
        val importer = createInstance()

        // When
        val nodes = importer.load(rootDirectory = tempDir, basePath = basePath).toList().blockingGet()

        // Then
        assertThat(nodes).isEmpty()
    }

    @Test
    fun `load, folders`() {
        // Given
        createFile(filename = "Title 1.md", path = "foo")
        createFile(filename = "Title 2.md", path = "foo/bar")
        createFile(filename = "Title 3.md", path = "foo/baz")
        val importer = createInstance()

        // When
        val nodes = importer.load(rootDirectory = tempDir, basePath = basePath).toList().blockingGet()

        // Then
        assertThat(nodes).hasSize(7)
        assertThat(nodes.toSet()).isEqualTo(setOf(
                ImportableNode.Folder(path = basePath),
                ImportableNode.Folder(path = basePath.child("foo")),
                ImportableNode.Folder(path = basePath.child("foo").child("bar")),
                ImportableNode.Folder(path = basePath.child("foo").child("baz")),
                ImportableNode.Note(path = basePath.child("foo"), title = "Title 1", content = ""),
                ImportableNode.Note(path = basePath.child("foo").child("bar"), title = "Title 2", content = ""),
                ImportableNode.Note(path = basePath.child("foo").child("baz"), title = "Title 3", content = "")
        ))
    }

    @Test
    fun `load, convert line endings`() {
        // Given
        createFile(filename = "Title 1.md", content = "bla\r\nabc")
        createFile(filename = "Title 2.md", content = "bla\nabc")
        createFile(filename = "Title 3.md", content = "bla\rabc")
        val importedContent = "bla\nabc"
        val importer = createInstance()

        // When
        val nodes = importer.load(rootDirectory = tempDir, basePath = basePath).toList().blockingGet()

        // Then
        assertThat(nodes).hasSize(4)
        assertThat(nodes.toSet()).isEqualTo(setOf(
                ImportableNode.Folder(path = basePath),
                ImportableNode.Note(path = basePath, title = "Title 1", content = importedContent),
                ImportableNode.Note(path = basePath, title = "Title 2", content = importedContent),
                ImportableNode.Note(path = basePath, title = "Title 3", content = importedContent)
        ))
    }

    @Test
    fun `generate base path`() {
        // Given
        val importer = createInstance()

        // When
        val basePath = importer.newBasePathForCurrentTime()

        // Then
        assertThat(basePath).isEqualTo(Path(importFolderName))
    }

    // TODO
    // Test import method
//    verify {
//        commandProcessor.commands.onNext(CreateNoteCommand(aggId = null, path = Path(importFolderName), title = "Title", content = ""))
//    }
    // Test parent path generating method

    private fun createFile(filename: String, content: String = "", path: String = "") {
        val dir = if (path == "") {
            tempDir
        } else {
            tempDir.resolve(path)
        }
        dir.mkdirs()
        dir.resolve(filename).writeText(content)
    }

    private fun createInstance(): MarkdownFilesImporter {
        return MarkdownFilesImporter(commandProcessor, clock, zone)
    }
}