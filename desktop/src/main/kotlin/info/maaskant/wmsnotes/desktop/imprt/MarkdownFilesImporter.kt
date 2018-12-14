package info.maaskant.wmsnotes.desktop.imprt

import com.google.common.annotations.VisibleForTesting
import info.maaskant.wmsnotes.model.CommandProcessor
import java.io.File
import java.time.Clock
import javax.inject.Inject

class MarkdownFilesImporter @VisibleForTesting constructor(
        private val commandProcessor: CommandProcessor,
        private val clock: Clock
) {
    @Inject
    constructor(commandProcessor: CommandProcessor) : this(commandProcessor, Clock.systemDefaultZone())

    fun import(directory: File) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
