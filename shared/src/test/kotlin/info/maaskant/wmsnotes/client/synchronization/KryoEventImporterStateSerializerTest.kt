package info.maaskant.wmsnotes.client.synchronization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest

internal class KryoEventImporterStateSerializerTest : KryoSerializerTest<EventImporterState>() {
    override val items: List<EventImporterState> = listOf(
            EventImporterState(lastEventId = null),
            EventImporterState(lastEventId = 123)
    )

    override fun createInstance(kryoPool: Pool<Kryo>) = KryoEventImporterStateSerializer(kryoPool)
}
