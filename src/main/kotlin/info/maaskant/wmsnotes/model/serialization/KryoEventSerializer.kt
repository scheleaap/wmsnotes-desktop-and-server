package info.maaskant.wmsnotes.model.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.stream.Stream
import javax.inject.Inject

class KryoEventSerializer @Inject constructor(): EventSerializer {

    private val kryo = Kryo()

    init {
        Stream.of(
                Pair(NoteCreatedEvent::class.java, NoteCreatedEventSerializer()),
                Pair(NoteDeletedEvent::class.java, NoteDeletedEventSerializer())
        ).forEach {
            kryo.register(it.first, it.second)
        }
    }

    override fun serialize(event: Event): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        byteArrayOutputStream.use { baos ->
            Output(baos).use { ko -> kryo.writeClassAndObject(ko, event) }
        }
        return byteArrayOutputStream.toByteArray()
    }

    override fun deserialize(bytes: ByteArray): Event {
        return Input(bytes).use { ki ->
            kryo.readClassAndObject(ki) as Event
        }
    }
}

private class NoteCreatedEventSerializer : Serializer<NoteCreatedEvent>() {
    override fun write(kryo: Kryo, output: Output, it: NoteCreatedEvent) {
        output.writeInt(it.eventId, true)
        output.writeString(it.noteId)
        output.writeString(it.title)
    }

    override fun read(kryo: Kryo, input: Input, clazz: Class<out NoteCreatedEvent>): NoteCreatedEvent {
        val eventId = input.readInt(true)
        val id = input.readString()
        val title = input.readString()
        return NoteCreatedEvent(noteId = id, title = title, eventId = eventId)
    }
}

private class NoteDeletedEventSerializer : Serializer<NoteDeletedEvent>() {
    override fun write(kryo: Kryo, output: Output, it: NoteDeletedEvent) {
        output.writeInt(it.eventId, true)
        output.writeString(it.noteId)
    }

    override fun read(kryo: Kryo, input: Input, clazz: Class<out NoteDeletedEvent>): NoteDeletedEvent {
        val eventId = input.readInt(true)
        val id = input.readString()
        return NoteDeletedEvent(noteId = id, eventId = eventId)
    }
}

private class UuidSerializer : Serializer<UUID>() {
    override fun write(kryo: Kryo, output: Output, it: UUID) {
        with(output) {
            writeLong(it.mostSignificantBits)
            writeLong(it.leastSignificantBits)
        }
    }

    override fun read(kryo: Kryo, input: Input, clazz: Class<out UUID>): UUID {
        return UUID(input.readLong(), input.readLong())
    }
}
