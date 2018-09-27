package info.maaskant.wmsnotes.desktop.app

import com.esotericsoftware.kryo.Kryo
import info.maaskant.wmsnotes.model.EventStore
import info.maaskant.wmsnotes.model.synchronization.InboundSynchronizer
import info.maaskant.wmsnotes.model.Model
import info.maaskant.wmsnotes.model.serialization.EventSerializer
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

val kodein = Kodein {

    bind<Kryo>() with singleton { Kryo() }
    bind<EventSerializer>() with singleton { EventSerializer(instance()) }
    bind<EventStore>() with singleton { EventStore(instance()) }
    bind<InboundSynchronizer>() with singleton { InboundSynchronizer("localhost", 6565, instance(), instance()) }
    bind<Model>() with singleton { Model(instance()) }
}