package info.maaskant.wmsnotes.client.synchronization;

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.utilities.serialization.*
import javax.inject.Inject

data class SynchronizerState internal constructor(
        val lastSynchronizedLocalRevisions: Map<String, Int?>,
        val lastKnownLocalRevisions: Map<String, Int?>,
        val lastKnownRemoteRevisions: Map<String, Int?>,
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

    fun updateLastSynchronizedLocalRevision(aggId: String, revision: Int) =
            this.copy(lastSynchronizedLocalRevisions = lastSynchronizedLocalRevisions + (aggId to revision))

    fun updateLastKnownLocalRevision(aggId: String, revision: Int) =
            this.copy(lastKnownLocalRevisions = lastKnownLocalRevisions + (aggId to revision))

    fun updateLastKnownRemoteRevision(aggId: String, revision: Int) =
            this.copy(lastKnownRemoteRevisions = lastKnownRemoteRevisions + (aggId to revision))

    companion object {
        fun create(
                lastSynchronizedLocalRevisions: Map<String, Int?> = emptyMap(),
                lastKnownLocalRevisions: Map<String, Int?> = emptyMap(),
                lastKnownRemoteRevisions: Map<String, Int?> = emptyMap(),
                localEventIdsToIgnore: Set<Int> = emptySet(),
                remoteEventIdsToIgnore: Set<Int> = emptySet()
        ) = SynchronizerState(
                lastSynchronizedLocalRevisions = lastSynchronizedLocalRevisions.withDefault { null },
                lastKnownLocalRevisions = lastKnownLocalRevisions.withDefault { null },
                lastKnownRemoteRevisions = lastKnownRemoteRevisions.withDefault { null },
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
            output.writeMapWithNullableValues(it.lastSynchronizedLocalRevisions) { key, value ->
                output.writeString(key)
                output.writeNullableInt(value)
            }
            output.writeMapWithNullableValues(it.lastKnownLocalRevisions) { key, value ->
                output.writeString(key)
                output.writeNullableInt(value)
            }
            output.writeMapWithNullableValues(it.lastKnownRemoteRevisions) { key, value ->
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
            val lastSynchronizedLocalRevisions = input.readMapWithNullableValues<String, Int?> {
                input.readString() to input.readNullableInt(true)
            }
            val lastKnownLocalRevisions = input.readMapWithNullableValues<String, Int?> {
                input.readString() to input.readNullableInt(true)
            }
            val lastKnownRemoteRevisions = input.readMapWithNullableValues<String, Int?> {
                input.readString() to input.readNullableInt(true)
            }
            val localEventIdsToIgnore = input.readSet<Int> {
                input.readInt(true)
            }
            val remoteEventIdsToIgnore = input.readSet<Int> {
                input.readInt(true)
            }
            return SynchronizerState.create(
                    lastSynchronizedLocalRevisions = lastSynchronizedLocalRevisions,
                    lastKnownLocalRevisions = lastKnownLocalRevisions,
                    lastKnownRemoteRevisions = lastKnownRemoteRevisions,
                    localEventIdsToIgnore = localEventIdsToIgnore,
                    remoteEventIdsToIgnore = remoteEventIdsToIgnore
            )
        }
    }
}
