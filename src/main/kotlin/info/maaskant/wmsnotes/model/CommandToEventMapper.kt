package info.maaskant.wmsnotes.model

import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandToEventMapper @Inject constructor() {
    fun map(command: Command): Event {
        return when (command) {
            is CreateNoteCommand -> {
                val noteId = command.noteId ?: UUID.randomUUID().toString()
                NoteCreatedEvent(eventId = 0, noteId = noteId, revision = 1, title = command.title)
            }
            is DeleteNoteCommand -> NoteDeletedEvent(eventId = 0, noteId = command.noteId, revision = command.lastRevision + 1)
        }
    }
}
