package info.maaskant.wmsnotes.model.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import info.maaskant.wmsnotes.model.*
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.stream.Stream

class EventSerializer(private val kryo: Kryo) {

    init {
        Stream.of(
                Pair(UUID::class.java, UuidSerializer()),
                Pair(NoteCreatedEvent::class.java, NoteCreatedEventSerializer()),
                Pair(NoteDeletedEvent::class.java, NoteDeletedEventSerializer())
        ).forEach {
            kryo.register(it.first, it.second)
//            kryo.addDefaultSerializer(it.first, it.second)
        }
    }

    fun serialize(event: Event): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        byteArrayOutputStream.use { baos ->
            Output(baos).use { ko -> kryo.writeClassAndObject(ko, event) }
        }
        return byteArrayOutputStream.toByteArray()
    }

    fun deserialize(bytes: ByteArray): Event {
        return Input(bytes).use { ki ->
            kryo.readClassAndObject(ki) as Event
        }
    }
}

