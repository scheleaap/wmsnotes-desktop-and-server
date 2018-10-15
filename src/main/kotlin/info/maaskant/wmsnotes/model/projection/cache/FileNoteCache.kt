package info.maaskant.wmsnotes.model.projection.cache

import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.model.projection.Note
import java.io.File

class FileNoteCache(private val rootDirectory: File, private val serializer: NoteSerializer) : CachingNoteProjector.NoteCache {
    private val logger by logger()

    override fun get(noteId: String, revision: Int): Note? {
        val noteFilePath = noteFilePath(noteId, revision)
        return serializer.deserialize(noteFilePath.readBytes())
    }

    override fun put(note: Note) {
        val noteFilePath = noteFilePath(note)

        logger.debug("Storing note ${note.noteId} revision ${note.revision}, saving to $noteFilePath")
        noteFilePath.parentFile.mkdirs()
        noteFilePath.writeBytes(serializer.serialize(note))
    }

    private fun noteFilePath(noteId: String, revision: Int): File = rootDirectory.resolve("$noteId.$revision")
    private fun noteFilePath(note: Note): File = noteFilePath(noteId = note.noteId, revision = note.revision)

    interface NoteSerializer {
        fun serialize(note: Note): ByteArray
        fun deserialize(bytes: ByteArray): Note
    }
}
