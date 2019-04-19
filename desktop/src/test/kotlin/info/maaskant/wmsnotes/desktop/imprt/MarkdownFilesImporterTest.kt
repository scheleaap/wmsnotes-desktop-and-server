package info.maaskant.wmsnotes.desktop.imprt

import info.maaskant.wmsnotes.desktop.imprt.MarkdownFilesImporter.ImportableNode
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.folder.CreateFolderCommand
import info.maaskant.wmsnotes.model.note.CreateNoteCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime

internal class MarkdownFilesImporterTest {
    private val zone = ZoneId.of("UTC")
    private val clock: Clock = Clock.fixed(ZonedDateTime.of(2001, 2, 3, 4, 5, 6, 7, zone).toInstant(), zone)
    private val importFolderName: String = "Import of 2001-02-03 04:05:06"
    private val basePath1 = Path("a")
    private val basePath2 = Path("a", "b")

    private lateinit var tempDir: File

    @BeforeEach
    fun init() {
        tempDir = createTempDir(this::class.simpleName!!).resolve("files")
    }

    @Test
    fun `load, empty file`() {
        // Given
        createFile(filename = "Title.md")

        // When
        val nodes = MarkdownFilesImporter.load(rootDirectory = tempDir, basePath = basePath2).toList().blockingGet()

        // Then
        assertThat(nodes).hasSize(3)
        assertThat(nodes.toSet()).isEqualTo(setOf(
                ImportableNode.Folder(path = basePath1),
                ImportableNode.Folder(path = basePath2),
                ImportableNode.Note(path = basePath2, title = "Title", content = "")
        ))
    }

    @Test
    fun `load, file with content`() {
        // Given
        val content = "Content"
        createFile(filename = "Title.md", content = content)

        // When
        val nodes = MarkdownFilesImporter.load(rootDirectory = tempDir, basePath = basePath2).toList().blockingGet()

        // Then
        assertThat(nodes).hasSize(3)
        assertThat(nodes.toSet()).isEqualTo(setOf(
                ImportableNode.Folder(path = basePath1),
                ImportableNode.Folder(path = basePath2),
                ImportableNode.Note(path = basePath2, title = "Title", content = content)
        ))
    }

    @Test
    fun `load, multiple files with content`() {
        // Given
        createFile(filename = "Title 1.md", content = "Content 1")
        createFile(filename = "Title 2.md", content = "Content 2")

        // When
        val nodes = MarkdownFilesImporter.load(rootDirectory = tempDir, basePath = basePath2).toList().blockingGet()

        // Then
        assertThat(nodes).hasSize(4)
        assertThat(nodes.toSet()).isEqualTo(setOf(
                ImportableNode.Folder(path = basePath1),
                ImportableNode.Folder(path = basePath2),
                ImportableNode.Note(path = basePath2, title = "Title 1", content = "Content 1"),
                ImportableNode.Note(path = basePath2, title = "Title 2", content = "Content 2")
        ))
    }

    @Test
    fun `load, non-markdown files`() {
        // Given
        createFile(filename = "file.txt")
        createFile(filename = "file.jpg", path = "foo/bar")

        // When
        val nodes = MarkdownFilesImporter.load(rootDirectory = tempDir, basePath = basePath2).toList().blockingGet()

        // Then
        assertThat(nodes).isEmpty()
    }

    @Test
    fun `load, folders`() {
        // Given
        createFile(filename = "Title 1.md", path = "foo")
        createFile(filename = "Title 2.md", path = "foo/bar")
        createFile(filename = "Title 3.md", path = "foo/baz")

        // When
        val nodes = MarkdownFilesImporter.load(rootDirectory = tempDir, basePath = basePath2).toList().blockingGet()

        // Then
        assertThat(nodes).hasSize(8)
        assertThat(nodes.toSet()).isEqualTo(setOf(
                ImportableNode.Folder(path = basePath1),
                ImportableNode.Folder(path = basePath2),
                ImportableNode.Folder(path = basePath2.child("foo")),
                ImportableNode.Folder(path = basePath2.child("foo").child("bar")),
                ImportableNode.Folder(path = basePath2.child("foo").child("baz")),
                ImportableNode.Note(path = basePath2.child("foo"), title = "Title 1", content = ""),
                ImportableNode.Note(path = basePath2.child("foo").child("bar"), title = "Title 2", content = ""),
                ImportableNode.Note(path = basePath2.child("foo").child("baz"), title = "Title 3", content = "")
        ))
    }

    @Test
    fun `load, convert line endings`() {
        // Given
        createFile(filename = "Title 1.md", content = "bla\r\nabc")
        createFile(filename = "Title 2.md", content = "bla\nabc")
        createFile(filename = "Title 3.md", content = "bla\rabc")
        val importedContent = "bla\nabc"

        // When
        val nodes = MarkdownFilesImporter.load(rootDirectory = tempDir, basePath = basePath2).toList().blockingGet()

        // Then
        assertThat(nodes).hasSize(5)
        assertThat(nodes.toSet()).isEqualTo(setOf(
                ImportableNode.Folder(path = basePath1),
                ImportableNode.Folder(path = basePath2),
                ImportableNode.Note(path = basePath2, title = "Title 1", content = importedContent),
                ImportableNode.Note(path = basePath2, title = "Title 2", content = importedContent),
                ImportableNode.Note(path = basePath2, title = "Title 3", content = importedContent)
        ))
    }

    @Test
    fun `generate base path`() {
        // Given
        val importer = createInstance()

        // When
        val basePath = importer.basePathForCurrentTime()

        // Then
        assertThat(basePath).isEqualTo(Path(importFolderName))
    }

    @Test
    fun `map note to command`() {
        // Given
        val node = ImportableNode.Note(path = basePath2, title = "Title", content = "Content")

        // When
        val command = MarkdownFilesImporter.mapToCommand(node)

        // Then
        assertThat(command).isInstanceOf(CreateNoteCommand::class.java)
        val command2 = command as CreateNoteCommand
        assertThat(command2.path).isEqualTo(basePath2)
        assertThat(command2.title).isEqualTo("Title")
        assertThat(command2.content).isEqualTo("Content")
    }

    @Test
    fun `map folder to command`() {
        // Given
        val node = ImportableNode.Folder(path = basePath2)

        // When
        val command = MarkdownFilesImporter.mapToCommand(node)

        // Then
        assertThat(command).isEqualTo(CreateFolderCommand(path = basePath2))
    }

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
        return MarkdownFilesImporter(clock, zone)
    }
}