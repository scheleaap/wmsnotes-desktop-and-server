package info.maaskant.wmsnotes.model.projection.cache

import info.maaskant.wmsnotes.utilities.logger
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.utilities.serialization.Serializer
import java.io.File

class FileNoteCache(private val rootDirectory: File, private val serializer: Serializer<Note>) : NoteCache {
    private val logger by logger()

    override fun get(noteId: String, revision: Int): Note? {
        val noteFilePath = noteFilePath(noteId, revision)
        return if (noteFilePath.exists()) {
            serializer.deserialize(noteFilePath.readBytes())
        } else {
            null
        }
    }

    override fun getLatest(noteId: String, lastRevision: Int?): Note? {
        val lastRevisionFileName: String? = if (lastRevision != null) {
            "%010d".format(lastRevision)
        } else {
            null
        }
        val revision: Int? = noteDirectoryPath(noteId)
                .walkTopDown()
                .filter { it.isFile }
                .map { it.name }
                .sortedBy { it }
                .filter { lastRevisionFileName == null || it <= lastRevisionFileName }
                .lastOrNull()
                ?.toInt()
        return if (revision != null) {
            get(noteId, revision)
        } else {
            null
        }
    }

    override fun put(note: Note) {
        val noteFilePath = noteFilePath(note)

        logger.debug("Storing note ${note.noteId} revision ${note.revision}, saving to $noteFilePath")
        noteFilePath.parentFile.mkdirs()
        noteFilePath.writeBytes(serializer.serialize(note))
    }

    override fun remove(noteId: String, revision: Int) {
        val noteFilePath = noteFilePath(noteId, revision)

        if (noteFilePath.exists()) {
            logger.debug("Removing note $noteId revision $revision, deleting $noteFilePath")
            noteFilePath.delete()
        }
    }

    private fun noteFilePath(noteId: String, revision: Int): File = rootDirectory.resolve(noteId).resolve("%010d".format(revision))
    private fun noteFilePath(note: Note): File = noteFilePath(noteId = note.noteId, revision = note.revision)
    private fun noteDirectoryPath(noteId: String) = rootDirectory.resolve(noteId)

}
