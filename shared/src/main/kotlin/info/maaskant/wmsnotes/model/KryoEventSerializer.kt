package info.maaskant.wmsnotes.model

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializer

class KryoEventSerializer(kryoPool: Pool<Kryo>) : KryoSerializer<Event>(
        kryoPool,
        Registration(NoteCreatedEvent::class.java, NoteCreatedEventSerializer(), 11),
        Registration(NoteDeletedEvent::class.java, NoteDeletedEventSerializer(), 12),
        Registration(NoteUndeletedEvent::class.java, NoteUndeletedEventSerializer(), 13),
        Registration(AttachmentAddedEvent::class.java, AttachmentAddedEventSerializer(), 14),
        Registration(AttachmentDeletedEvent::class.java, AttachmentDeletedEventSerializer(), 15),
        Registration(ContentChangedEvent::class.java, ContentChangedEventSerializer(), 16),
        Registration(TitleChangedEvent::class.java, TitleChangedEventSerializer(), 17),
        Registration(MovedEvent::class.java, MovedEventSerializer(), 18)
) {
    private class NoteCreatedEventSerializer : Serializer<NoteCreatedEvent>() {
        override fun write(kryo: Kryo, output: Output, it: NoteCreatedEvent) {
            output.writeInt(it.eventId, true)
            output.writeString(it.noteId)
            output.writeInt(it.revision, true)
            output.writeString(it.path.toString())
            output.writeString(it.title)
            output.writeString(it.content)
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out NoteCreatedEvent>): NoteCreatedEvent {
            val eventId = input.readInt(true)
            val noteId = input.readString()
            val revision = input.readInt(true)
            val path = Path.from(input.readString())
            val title = input.readString()
            val content = input.readString()
            return NoteCreatedEvent(eventId = eventId, noteId = noteId, revision = revision, path = path, title = title, content = content)
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
            val noteId = input.readString()
            val revision = input.readInt(true)
            return NoteDeletedEvent(eventId = eventId, noteId = noteId, revision = revision)
        }
    }

    private class NoteUndeletedEventSerializer : Serializer<NoteUndeletedEvent>() {
        override fun write(kryo: Kryo, output: Output, it: NoteUndeletedEvent) {
            output.writeInt(it.eventId, true)
            output.writeString(it.noteId)
            output.writeInt(it.revision, true)
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out NoteUndeletedEvent>): NoteUndeletedEvent {
            val eventId = input.readInt(true)
            val noteId = input.readString()
            val revision = input.readInt(true)
            return NoteUndeletedEvent(eventId = eventId, noteId = noteId, revision = revision)
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
            val noteId = input.readString()
            val revision = input.readInt(true)
            val name = input.readString()
            val contentLength = input.readInt(true)
            val content = input.readBytes(contentLength)
            return AttachmentAddedEvent(eventId = eventId, noteId = noteId, revision = revision, name = name, content = content)
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
            val noteId = input.readString()
            val revision = input.readInt(true)
            val name = input.readString()
            return AttachmentDeletedEvent(eventId = eventId, noteId = noteId, revision = revision, name = name)
        }
    }

    private class ContentChangedEventSerializer : Serializer<ContentChangedEvent>() {
        override fun write(kryo: Kryo, output: Output, it: ContentChangedEvent) {
            output.writeInt(it.eventId, true)
            output.writeString(it.noteId)
            output.writeInt(it.revision, true)
            output.writeString(it.content)
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out ContentChangedEvent>): ContentChangedEvent {
            val eventId = input.readInt(true)
            val noteId = input.readString()
            val revision = input.readInt(true)
            val content = input.readString()
            return ContentChangedEvent(eventId = eventId, noteId = noteId, revision = revision, content = content)
        }
    }

    private class TitleChangedEventSerializer : Serializer<TitleChangedEvent>() {
        override fun write(kryo: Kryo, output: Output, it: TitleChangedEvent) {
            output.writeInt(it.eventId, true)
            output.writeString(it.noteId)
            output.writeInt(it.revision, true)
            output.writeString(it.title)
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out TitleChangedEvent>): TitleChangedEvent {
            val eventId = input.readInt(true)
            val noteId = input.readString()
            val revision = input.readInt(true)
            val title = input.readString()
            return TitleChangedEvent(eventId = eventId, noteId = noteId, revision = revision, title = title)
        }
    }

    private class MovedEventSerializer : Serializer<MovedEvent>() {
        override fun write(kryo: Kryo, output: Output, it: MovedEvent) {
            output.writeInt(it.eventId, true)
            output.writeString(it.noteId)
            output.writeInt(it.revision, true)
            output.writeString(it.path.toString())
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out MovedEvent>): MovedEvent {
            val eventId = input.readInt(true)
            val noteId = input.readString()
            val revision = input.readInt(true)
            val path = Path.from(input.readString())
            return MovedEvent(eventId = eventId, noteId = noteId, revision = revision, path = path)
        }
    }
}
