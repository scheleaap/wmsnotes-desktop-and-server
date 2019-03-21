package info.maaskant.wmsnotes.model.folder

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import info.maaskant.wmsnotes.model.Path

class FolderCreatedEventSerializer : Serializer<FolderCreatedEvent>() {
    override fun write(kryo: Kryo, output: Output, it: FolderCreatedEvent) {
        output.writeInt(it.eventId, true)
        output.writeInt(it.revision, true)
        output.writeString(it.path.toString())
    }

    override fun read(kryo: Kryo, input: Input, clazz: Class<out FolderCreatedEvent>): FolderCreatedEvent {
        val eventId = input.readInt(true)
        val revision = input.readInt(true)
        val path = Path.from(input.readString())
        return FolderCreatedEvent(eventId = eventId, revision = revision, path = path)
    }
}

class FolderDeletedEventSerializer : Serializer<FolderDeletedEvent>() {
    override fun write(kryo: Kryo, output: Output, it: FolderDeletedEvent) {
        output.writeInt(it.eventId, true)
        output.writeInt(it.revision, true)
        output.writeString(it.path.toString())
    }

    override fun read(kryo: Kryo, input: Input, clazz: Class<out FolderDeletedEvent>): FolderDeletedEvent {
        val eventId = input.readInt(true)
        val revision = input.readInt(true)
        val path = Path.from(input.readString())
        return FolderDeletedEvent(eventId = eventId, revision = revision, path = path)
    }
}
