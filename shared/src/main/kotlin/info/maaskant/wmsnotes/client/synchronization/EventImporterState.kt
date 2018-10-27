package info.maaskant.wmsnotes.client.synchronization;

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializer
import info.maaskant.wmsnotes.utilities.serialization.readNullableInt
import info.maaskant.wmsnotes.utilities.serialization.writeNullableInt
import javax.inject.Inject

data class EventImporterState(val lastEventId: Int?)

class KryoEventImporterStateSerializer @Inject constructor(kryoPool: Pool<Kryo>) : KryoSerializer<EventImporterState>(
        kryoPool,
        Registration(EventImporterState::class.java, KryoEventImporterStateSerializer(),31)
) {

    private class KryoEventImporterStateSerializer : Serializer<EventImporterState>() {
        override fun write(kryo: Kryo, output: Output, it: EventImporterState) {
            output.writeNullableInt(it.lastEventId)
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out EventImporterState>): EventImporterState {
            val lastEventId = input.readNullableInt(true)
            return EventImporterState(lastEventId)
        }
    }
}