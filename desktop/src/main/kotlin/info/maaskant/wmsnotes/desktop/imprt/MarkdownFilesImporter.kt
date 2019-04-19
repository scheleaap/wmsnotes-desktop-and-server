package info.maaskant.wmsnotes.desktop.imprt

import com.google.common.annotations.VisibleForTesting
import info.maaskant.wmsnotes.model.AggregateCommand
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.folder.CreateFolderCommand
import info.maaskant.wmsnotes.model.note.CreateNoteCommand
import info.maaskant.wmsnotes.model.note.Note
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import java.io.File
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class MarkdownFilesImporter @VisibleForTesting constructor(
        private val clock: Clock,
        zone: ZoneId
) {
    private val dateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd hh:mm:ss")
            .withZone(zone)

    @Inject
    constructor() : this(Clock.systemDefaultZone(), ZoneId.systemDefault())

    fun basePathForCurrentTime(): Path {
        return Path("Import of " + dateTimeFormatter.format(clock.instant()))
    }

    companion object {
        private const val markdownFileSuffix = ".md"

        private fun isImportable(item: File, rootDirectory: File) =
                (item.isFile && item.name.endsWith(markdownFileSuffix))
                        || (item.isDirectory && item != rootDirectory)

        private fun contentFromFile(file: File) =
                file.useLines { it.joinToString(separator = "\n") }

        fun load(rootDirectory: File, basePath: Path): Observable<ImportableNode> {
            return rootDirectory.walkTopDown().toObservable()
                    .filter { isImportable(it, rootDirectory) }
                    .flatMap { file ->
                        when {
                            file.isFile -> {
                                val notePath = notePathFromFile(file, rootDirectory, basePath)
                                Observable.concat(
                                        Observable.just(ImportableNode.Note(
                                                path = notePath,
                                                title = titleFromFile(file),
                                                content = contentFromFile(file)
                                        )),
                                        parentPathsOf(notePath).toObservable()
                                                .map { ImportableNode.Folder(path = it) }
                                )
                            }
                            else -> Observable.empty()
                        }
                    }
                    .map { setOf(it) }
                    .scan(Pair(emptySet(), emptySet())) { previousResult: Pair<Set<Path>, Set<ImportableNode>>, it: Set<ImportableNode> ->
                        // The pair's first element is a set of all paths for which a folder has been created already
                        // The pair's second element is the current node to process, wrapped in a set. The reason for wrapping
                        //  it in a set is that this allows us to return an empty set in case we want to filter out the node.
                        //  This is a workaround because we cannot do scan() and flatMap() at the same time.
                        when (val node: ImportableNode = it.first()) {
                            is ImportableNode.Folder -> if (node.path in previousResult.first) {
                                previousResult.first to emptySet()
                            } else {
                                previousResult.first + node.path to it
                            }
                            else -> previousResult.first to it
                        }
                    }
                    .skip(1) // Skip the first, fake ImportableNode.Folder
                    .flatMap { it.second.toObservable() }
        }

        private fun notePathFromFile(file: File, rootDirectory: File, basePath: Path): Path {
            val parentDirectory = file.parentFile
            return if (parentDirectory == rootDirectory) {
                basePath
            } else {
                val fileSystemBasedPathString = parentDirectory.canonicalPath
                        .removePrefix(rootDirectory.canonicalPath)
                        .substring(1)
                val pathElements = basePath.elements + fileSystemBasedPathString.split(File.separatorChar)
                Path(pathElements)
            }
        }

        private fun parentPathsOf(path: Path): List<Path> {
            return if (!path.isRoot) {
                parentPathsOf(path.parent()) + path
            } else {
                emptyList()
            }
        }

        private fun titleFromFile(file: File) =
                file.nameWithoutExtension

        fun mapToCommand(node: ImportableNode): AggregateCommand =
                when (node) {
                    is ImportableNode.Folder -> CreateFolderCommand(path = node.path)
                    is ImportableNode.Note -> CreateNoteCommand(Note.randomAggId(), path = node.path, title = node.title, content = node.content)
                }
    }

    sealed class ImportableNode {
        data class Folder(val path: Path) : ImportableNode()
        data class Note(
                val path: Path,
                val title: String,
                val content: String
        ) : ImportableNode()
    }
}
