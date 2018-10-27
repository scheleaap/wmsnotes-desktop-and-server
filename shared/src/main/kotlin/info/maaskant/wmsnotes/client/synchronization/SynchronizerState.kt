package info.maaskant.wmsnotes.client.synchronization;

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.client.indexing.NoteMetadata
import info.maaskant.wmsnotes.utilities.serialization.*
import javax.inject.Inject

data class SynchronizerState internal constructor(
        val lastLocalRevisions: Map<String, Int?>,
        val lastRemoteRevisions: Map<String, Int?>,
        val localEventIdsToIgnore: Set<Int>,
        val remoteEventIdsToIgnore: Set<Int>
) {
    fun ignoreLocalEvent(eventId: Int) =
            this.copy(localEventIdsToIgnore = localEventIdsToIgnore + eventId)

    fun ignoreRemoteEvent(eventId: Int) =
            this.copy(remoteEventIdsToIgnore = remoteEventIdsToIgnore + eventId)

    fun removeLocalEventToIgnore(eventId: Int) =
            this.copy(localEventIdsToIgnore = localEventIdsToIgnore - eventId)

    fun removeRemoteEventToIgnore(eventId: Int) =
            this.copy(remoteEventIdsToIgnore = remoteEventIdsToIgnore - eventId)

    fun updateLastLocalRevision(noteId: String, revision: Int) =
            this.copy(lastLocalRevisions = lastLocalRevisions + (noteId to revision))

    fun updateLastRemoteRevision(noteId: String, revision: Int) =
            this.copy(lastRemoteRevisions = lastRemoteRevisions + (noteId to revision))

    companion object {
        fun create(
                lastLocalRevisions: Map<String, Int?> = emptyMap<String, Int?>(),
                lastRemoteRevisions: Map<String, Int?> = emptyMap<String, Int?>().withDefault { null },
                localEventIdsToIgnore: Set<Int> = emptySet<Int>(),
                remoteEventIdsToIgnore: Set<Int> = emptySet<Int>()
        ) = SynchronizerState(
                lastLocalRevisions = lastLocalRevisions.withDefault { null },
                lastRemoteRevisions = lastRemoteRevisions.withDefault { null },
                localEventIdsToIgnore = localEventIdsToIgnore,
                remoteEventIdsToIgnore = remoteEventIdsToIgnore
        )
    }
}

class KryoSynchronizerStateSerializer @Inject constructor(kryoPool: Pool<Kryo>) : KryoSerializer<SynchronizerState>(
        kryoPool,
        Registration(SynchronizerState::class.java, KryoSynchronizerStateSerializer(), 41)
) {

    private class KryoSynchronizerStateSerializer : Serializer<SynchronizerState>() {
        override fun write(kryo: Kryo, output: Output, it: SynchronizerState) {
            output.writeMap(it.lastLocalRevisions) { key, value ->
                output.writeString(key)
                output.writeNullableInt(value)
            }
            output.writeMap(it.lastRemoteRevisions) { key, value ->
                output.writeString(key)
                output.writeNullableInt(value)
            }
            output.writeSet(it.localEventIdsToIgnore) {
                output.writeInt(it, true)
            }
            output.writeSet(it.remoteEventIdsToIgnore) {
                output.writeInt(it, true)
            }
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out SynchronizerState>): SynchronizerState {
            val lastLocalRevisions = input.readMap<String, Int?> {
                input.readString() to input.readNullableInt(true)
            }
            val lastRemoteRevisions = input.readMap<String, Int?> {
                input.readString() to input.readNullableInt(true)
            }
            val localEventIdsToIgnore = input.readSet<Int> {
                input.readInt(true)
            }
            val remoteEventIdsToIgnore = input.readSet<Int> {
                input.readInt(true)
            }
            return SynchronizerState.create(
                    lastLocalRevisions = lastLocalRevisions,
                    lastRemoteRevisions = lastRemoteRevisions,
                    localEventIdsToIgnore = localEventIdsToIgnore,
                    remoteEventIdsToIgnore = remoteEventIdsToIgnore
            )
        }
    }
}
