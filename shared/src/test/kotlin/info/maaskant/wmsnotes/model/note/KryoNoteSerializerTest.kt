package info.maaskant.wmsnotes.model.note

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest

internal class KryoNoteSerializerTest : KryoSerializerTest<Note>() {
    private val aggId = "note"

    override val items: List<Note> = listOf(Note()
            .apply(NoteCreatedEvent(eventId = 1, aggId = aggId, revision = 1, path = Path("path"), title = "Title", content = "Text")).component1()
            .apply(AttachmentAddedEvent(eventId = 2, aggId = aggId, revision = 2, name = "att-1", content = "data1".toByteArray())).component1()
            .apply(AttachmentAddedEvent(eventId = 3, aggId = aggId, revision = 3, name = "att-2", content = "data2".toByteArray())).component1()
            .apply(ContentChangedEvent(eventId = 4, aggId = aggId, revision = 4, content = "Hello")).component1()
    )

    override fun createInstance(kryoPool: Pool<Kryo>) = KryoNoteSerializer(kryoPool)
}