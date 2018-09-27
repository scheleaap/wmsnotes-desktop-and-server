package info.maaskant.wmsnotes.desktop.app

import com.esotericsoftware.kryo.Kryo
import info.maaskant.wmsnotes.model.EventStore
import info.maaskant.wmsnotes.model.Model
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

val kodein = Kodein {
    bind<Kryo>() with singleton { Kryo() }
    bind<EventStore>() with singleton { EventStore(instance()) }
    bind<Model>() with singleton { Model(instance()) }
}