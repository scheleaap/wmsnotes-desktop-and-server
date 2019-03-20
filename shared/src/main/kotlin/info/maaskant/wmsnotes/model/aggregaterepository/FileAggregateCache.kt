package info.maaskant.wmsnotes.model.aggregaterepository

import info.maaskant.wmsnotes.model.Aggregate
import info.maaskant.wmsnotes.utilities.logger
import info.maaskant.wmsnotes.utilities.serialization.Serializer
import java.io.File

class FileAggregateCache<T : Aggregate<T>>(private val rootDirectory: File, private val serializer: Serializer<T>) : AggregateCache<T> {
    private val logger by logger()

    override fun get(aggId: String, revision: Int): T? {
        val noteFilePath = noteFilePath(aggId, revision)
        return if (noteFilePath.exists()) {
            serializer.deserialize(noteFilePath.readBytes())
        } else {
            null
        }
    }

    override fun getLatest(aggId: String, lastRevision: Int?): T? {
        val lastRevisionFileName: String? = if (lastRevision != null) {
            "%010d".format(lastRevision)
        } else {
            null
        }
        val revision: Int? = noteDirectoryPath(aggId)
                .walkTopDown()
                .filter { it.isFile }
                .map { it.name }
                .sortedBy { it }
                .filter { lastRevisionFileName == null || it <= lastRevisionFileName }
                .lastOrNull()
                ?.toInt()
        return if (revision != null) {
            get(aggId, revision)
        } else {
            null
        }
    }

    override fun put(note: T) {
        val noteFilePath = noteFilePath(note)

        if (!noteFilePath.exists()) {
            logger.debug("Storing note ${note.aggId} revision ${note.revision}, saving to $noteFilePath")
            noteFilePath.parentFile.mkdirs()
            noteFilePath.writeBytes(serializer.serialize(note))
        }
    }

    override fun remove(aggId: String, revision: Int) {
        val noteFilePath = noteFilePath(aggId, revision)

        if (noteFilePath.exists()) {
            logger.debug("Removing note $aggId revision $revision, deleting $noteFilePath")
            noteFilePath.delete()
        }
    }

    private fun noteFilePath(aggId: String, revision: Int): File = rootDirectory.resolve(aggId).resolve("%010d".format(revision))
    private fun noteFilePath(note: T): File = noteFilePath(aggId = note.aggId, revision = note.revision)
    private fun noteDirectoryPath(aggId: String) = rootDirectory.resolve(aggId)
}
