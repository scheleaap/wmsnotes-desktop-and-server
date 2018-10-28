package info.maaskant.wmsnotes.utilities.serialization

interface Serializer<T> {
    fun serialize(o: T): ByteArray
    fun deserialize(bytes: ByteArray): T
}
