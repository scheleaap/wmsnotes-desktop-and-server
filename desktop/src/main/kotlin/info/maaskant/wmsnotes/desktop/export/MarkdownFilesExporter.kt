package info.maaskant.wmsnotes.desktop.export

import info.maaskant.wmsnotes.client.indexing.TreeIndex
import info.maaskant.wmsnotes.desktop.main.editing.preview.AttachmentLinkResolver
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.utilities.logger
import io.reactivex.rxkotlin.subscribeBy
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Component
class MarkdownFilesExporter @Inject constructor(
    private val treeIndex: TreeIndex,
    private val noteRepository: AggregateRepository<Note>
) {
    private val logger by logger()

    fun export(rootDirectory: Path) {
        treeIndex.getNodes()
            .map { it.value }
            .filter { it is info.maaskant.wmsnotes.client.indexing.Note }
            .map { noteRepository.getLatest(it.aggId) }
            .subscribeBy(
                onNext = { note ->
                    val noteParentDirectory = note.path.elements.fold(
                        rootDirectory,
                        { directory, folderPathElement -> directory.resolve(folderPathElement) })
                    Files.createDirectories(noteParentDirectory)

                    val nodeFile = noteParentDirectory.resolve(note.title + markdownFileSuffix)
                    val markdown = note.content.replace(AttachmentLinkResolver.attachmentPrefix, "")
                    Files.writeString(nodeFile, markdown)

                    note.attachments.forEach { (name, content) ->
                        val attachmentFile = noteParentDirectory.resolve(name)
                        Files.write(attachmentFile, content)
                    }
                },
                onError = { logger.error("Error", it) }
            )
    }

    companion object {
        private const val markdownFileSuffix = ".md"
    }
}