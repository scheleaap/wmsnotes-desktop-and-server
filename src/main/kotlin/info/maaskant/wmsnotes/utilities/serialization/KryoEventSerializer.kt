package info.maaskant.wmsnotes.utilities.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import info.maaskant.wmsnotes.model.*
import java.io.ByteArrayOutputStream
import java.util.stream.Stream
import javax.inject.Inject

class KryoEventSerializer @Inject constructor() : EventSerializer {

    private val kryo = Kryo()

    init {
        Stream.of(
                Pair(NoteCreatedEvent::class.java, NoteCreatedEventSerializer()),
                Pair(NoteDeletedEvent::class.java, NoteDeletedEventSerializer()),
                Pair(AttachmentAddedEvent::class.java, AttachmentAddedEventSerializer()),
                Pair(AttachmentDeletedEvent::class.java, AttachmentDeletedEventSerializer())
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

    private class NoteCreatedEventSerializer : Serializer<NoteCreatedEvent>() {
        override fun write(kryo: Kryo, output: Output, it: NoteCreatedEvent) {
            output.writeInt(it.eventId, true)
            output.writeString(it.noteId)
            output.writeInt(it.revision, true)
            output.writeString(it.title)
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out NoteCreatedEvent>): NoteCreatedEvent {
            val eventId = input.readInt(true)
            val id = input.readString()
            val revision = input.readInt(true)
            val title = input.readString()
            return NoteCreatedEvent(eventId = eventId, noteId = id, revision = revision, title = title)
        }
    }

    private class NoteDeletedEventSerializer : Serializer<NoteDeletedEvent>() {
        override fun write(kryo: Kryo, output: Output, it: NoteDeletedEvent) {
            output.writeInt(it.eventId, true)
            output.writeString(it.noteId)
            output.writeInt(it.revision, true)
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out NoteDeletedEvent>): NoteDeletedEvent {
            val eventId = input.readInt(true)
            val id = input.readString()
            val revision = input.readInt(true)
            return NoteDeletedEvent(eventId = eventId, noteId = id, revision = revision)
        }
    }

    private class AttachmentAddedEventSerializer : Serializer<AttachmentAddedEvent>() {
        override fun write(kryo: Kryo, output: Output, it: AttachmentAddedEvent) {
            output.writeInt(it.eventId, true)
            output.writeString(it.noteId)
            output.writeInt(it.revision, true)
            output.writeString(it.name)
            output.writeInt(it.content.size, true)
            output.writeBytes(it.content)
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out AttachmentAddedEvent>): AttachmentAddedEvent {
            val eventId = input.readInt(true)
            val id = input.readString()
            val revision = input.readInt(true)
            val name = input.readString()
            val contentLength = input.readInt(true)
            val content = input.readBytes(contentLength)
            return AttachmentAddedEvent(eventId = eventId, noteId = id, revision = revision, name = name, content = content)
        }
    }

    private class AttachmentDeletedEventSerializer : Serializer<AttachmentDeletedEvent>() {
        override fun write(kryo: Kryo, output: Output, it: AttachmentDeletedEvent) {
            output.writeInt(it.eventId, true)
            output.writeString(it.noteId)
            output.writeInt(it.revision, true)
            output.writeString(it.name)
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out AttachmentDeletedEvent>): AttachmentDeletedEvent {
            val eventId = input.readInt(true)
            val id = input.readString()
            val revision = input.readInt(true)
            val name = input.readString()
            return AttachmentDeletedEvent(eventId = eventId, noteId = id, revision = revision, name = name)
        }
    }
}
