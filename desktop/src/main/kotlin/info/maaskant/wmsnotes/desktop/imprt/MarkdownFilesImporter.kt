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

    private fun folderPathFromFile(file: File, rootDirectory: File, basePath: Path): Path {
        val fileSystemBasedPathString = file.canonicalPath.removePrefix(rootDirectory.canonicalPath).substring(1)
        val pathElements = basePath.elements + fileSystemBasedPathString.split(File.separatorChar)
        return Path(pathElements)
    }

    fun load(rootDirectory: File, basePath: Path): Observable<in ImportableNode> {
        return rootDirectory.walkTopDown().toObservable()
                .filter { isImportable(it, rootDirectory) }
                .flatMap {
                    when {
                        it.isFile -> Observable.just(ImportableNode.Note(
                                path = notePathFromFile(it, rootDirectory, basePath),
                                title = titleFromFile(it),
                                content = contentFromFile(it)
                        ))
                        it.isDirectory -> Observable.just(ImportableNode.Folder(path = folderPathFromFile(it, rootDirectory, basePath)))
                        else -> Observable.empty()
                    }
                }
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

    private fun titleFromFile(file: File) =
            file.nameWithoutExtension

    sealed class ImportableNode {
        data class Folder(val path: Path)
        data class Note(
                val path: Path,
                val title: String,
                val content: String
        )
    }
}
