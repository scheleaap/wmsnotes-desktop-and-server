package info.maaskant.wmsnotes.model.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest

internal class KryoEventSerializerTest : KryoSerializerTest<Event>() {
    override val items: List<Event> = listOf(
            NoteCreatedEvent(eventId = 1, noteId = "note-1", revision = 1, title = "Title 1"),
            NoteDeletedEvent(eventId = 1, noteId = "note-1", revision = 1),
            AttachmentAddedEvent(eventId = 1, noteId = "note-1", revision = 1, name = "att-1", content = "DATA".toByteArray()),
            AttachmentDeletedEvent(eventId = 1, noteId = "note-1", revision = 1, name = "att-1")
            // Add more classes here
    )

    override fun createInstance(kryoPool: Pool<Kryo>) = KryoEventSerializer(kryoPool)
}
