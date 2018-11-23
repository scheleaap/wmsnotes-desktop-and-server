package info.maaskant.wmsnotes.client.synchronization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest

internal class KryoSynchronizerStateSerializerTest : KryoSerializerTest<SynchronizerState>() {
    override val items: List<SynchronizerState> = listOf(
            SynchronizerState.create(
                    lastSynchronizedLocalRevisions = mapOf("note-2" to 1),
                    lastKnownLocalRevisions = mapOf("note-1" to 1, "note-2" to 2),
                    lastKnownRemoteRevisions = mapOf("note-3" to 3, "note-4" to 4),
                    localEventIdsToIgnore = setOf(1, 2, 3),
                    remoteEventIdsToIgnore = setOf(4, 5, 6)
            )
    )

    override fun createInstance(kryoPool: Pool<Kryo>) = KryoSynchronizerStateSerializer(kryoPool)
}
