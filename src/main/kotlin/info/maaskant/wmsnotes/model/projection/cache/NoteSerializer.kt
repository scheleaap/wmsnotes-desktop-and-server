package info.maaskant.wmsnotes.model.projection.cache

import info.maaskant.wmsnotes.model.projection.Note

interface NoteSerializer {
    fun serialize(note: Note): ByteArray
    fun deserialize(bytes: ByteArray): Note
}