package info.maaskant.wmsnotes.model.projection.cache

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.AttachmentAddedEvent
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest
import kotlin.reflect.KClass

internal class KryoNoteSerializerTest : KryoSerializerTest<Note>() {
    private val noteId = "note"

    override val items: List<Note> = listOf(Note()
            .apply(NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = "Title")).component1()
            .apply(AttachmentAddedEvent(eventId = 2, noteId = noteId, revision = 2, name = "att-1", content = "data1".toByteArray())).component1()
            .apply(AttachmentAddedEvent(eventId = 3, noteId = noteId, revision = 3, name = "att-2", content = "data2".toByteArray())).component1()
    )

    override fun createInstance(kryoPool: Pool<Kryo>) = KryoNoteSerializer(kryoPool)
}