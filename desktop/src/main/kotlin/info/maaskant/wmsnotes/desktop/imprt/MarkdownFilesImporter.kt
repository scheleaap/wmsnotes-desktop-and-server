package info.maaskant.wmsnotes.desktop.imprt

import com.google.common.annotations.VisibleForTesting
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.Path
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import java.io.File
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class MarkdownFilesImporter @VisibleForTesting constructor(
        private val commandProcessor: CommandProcessor,
        private val clock: Clock,
        private val zone: ZoneId
) {
    private val dateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd hh:mm:ss")
            .withZone(zone)
    private val markdownFileSuffix = ".md"

    @Inject
    constructor(commandProcessor: CommandProcessor) :
            this(commandProcessor, Clock.systemDefaultZone(), ZoneId.systemDefault())

    fun load(rootDirectory: File, basePath: Path): Observable<in ImportableNode> {
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
                                            .filter { folderPath -> folderPath.isChildOf(basePath) || folderPath == basePath }
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

    private fun contentFromFile(file: File) =
            file.useLines { it.joinToString(separator = "\n") }

    fun import(directory: File) {
        // create parent path
        // load
        // map to commands
        // send to command processor
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun isImportable(item: File, rootDirectory: File) =
            (item.isFile && item.name.endsWith(markdownFileSuffix))
                    || (item.isDirectory && item != rootDirectory)

    fun newBasePathForCurrentTime(): Path {
        return Path("Import of " + dateTimeFormatter.format(clock.instant()))
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
            listOf(path)
        }
    }

    private fun titleFromFile(file: File) =
            file.nameWithoutExtension

    sealed class ImportableNode {
        data class Folder(val path: Path) : ImportableNode()
        data class Note(
                val path: Path,
                val title: String,
                val content: String
        ) : ImportableNode()
    }
}
