package info.maaskant.wmsnotes.client.indexing

import info.maaskant.wmsnotes.utilities.logger
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import info.maaskant.wmsnotes.model.eventstore.EventStore
import io.reactivex.Observable
import io.reactivex.Scheduler
import org.mapdb.DB
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import org.mapdb.serializer.GroupSerializerObjectArray
import javax.inject.Inject

data class NoteMetadata(val noteId: String, val title: String)

class NoteIndex @Inject constructor(eventStore: EventStore, database: DB, scheduler: Scheduler) {
    private val logger by logger()

    private val isNoteIndexInitialized = database.atomicBoolean("isNoteIndexInitialized").createOrOpen()
    private val notes = database
            .treeMap("noteIndex", Serializer.STRING, MapDbNoteMetadataSerializer())
            .createOrOpen()

    init {
        var source = eventStore.getEventUpdates()
        if (!isNoteIndexInitialized.get()) {
            source = Observable.concat(
                    eventStore.getEvents()
                            .doOnSubscribe { logger.debug("Creating initial note index") }
                            .doOnComplete {
                                isNoteIndexInitialized.set(true)
                                logger.debug("Initial note index created")
                            },
                    source
            )
        }
        source
                .subscribeOn(scheduler)
                .subscribe({
                    when (it) {
                        is NoteCreatedEvent -> {
                            logger.debug("Adding note ${it.noteId} to index")
                            notes[it.noteId] = NoteMetadata(it.noteId, it.title)
                        }
                        is NoteDeletedEvent -> {
                            logger.debug("Removing note ${it.noteId} from index")
                            notes.remove(it.noteId)!!
                        }
                        else -> {
                        }
                    }
                }, { logger.warn("Error", it) }, { logger.info("Finished") })
    }

    fun getNotes(): Observable<NoteMetadata> {
        return Observable.fromIterable(notes.values)
    }
}

internal class MapDbNoteMetadataSerializer : GroupSerializerObjectArray<NoteMetadata>() {
    override fun deserialize(input: DataInput2, available: Int): NoteMetadata {
        val noteId = input.readUTF()
        val title = input.readUTF()
        return NoteMetadata(noteId = noteId, title = title)
    }

    override fun serialize(output: DataOutput2, it: NoteMetadata) {
        output.writeUTF(it.noteId)
        output.writeUTF(it.title)
    }

}
