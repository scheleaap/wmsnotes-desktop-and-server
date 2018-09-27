package info.maaskant.wmsnotes.model.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import java.util.*

internal class NoteCreatedEventSerializer : Serializer<NoteCreatedEvent>() {
    override fun write(kryo: Kryo, output: Output, it: NoteCreatedEvent) {
        with(output) {
            writeString(it.id)
            writeString(it.title)
        }
    }

    override fun read(kryo: Kryo, input: Input, clazz: Class<out NoteCreatedEvent>): NoteCreatedEvent {
        return with(input) {
            NoteCreatedEvent(readString(), readString())
        }
    }

}

internal class NoteDeletedEventSerializer : Serializer<NoteDeletedEvent>() {
    override fun write(kryo: Kryo, output: Output, it: NoteDeletedEvent) {
        with(output) {
            writeString(it.id)
        }
    }

    override fun read(kryo: Kryo, input: Input, clazz: Class<out NoteDeletedEvent>): NoteDeletedEvent {
        return with(input) {
            NoteDeletedEvent(readString())
        }
    }
}

internal class UuidSerializer : Serializer<UUID>() {
    override fun write(kryo: Kryo, output: Output, it: UUID) {
        with (output) {
            writeLong(it.mostSignificantBits)
            writeLong(it.leastSignificantBits)
        }
    }

    override fun read(kryo: Kryo, input: Input, clazz: Class<out UUID>): UUID {
        return UUID(input.readLong(), input.readLong())
    }
}
