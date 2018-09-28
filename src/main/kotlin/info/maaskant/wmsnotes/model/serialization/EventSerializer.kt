package info.maaskant.wmsnotes.model.serialization

import info.maaskant.wmsnotes.model.Event

interface EventSerializer {
    fun serialize(event: Event): ByteArray
    fun deserialize(bytes: ByteArray): Event
}
