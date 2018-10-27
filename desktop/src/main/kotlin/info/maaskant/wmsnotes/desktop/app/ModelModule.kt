package info.maaskant.wmsnotes.desktop.app

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import dagger.Module
import dagger.Provides
import info.maaskant.wmsnotes.desktop.app.Configuration.cache
import info.maaskant.wmsnotes.desktop.app.Configuration.delay
import info.maaskant.wmsnotes.desktop.app.Configuration.storeInMemory
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.KryoEventSerializer
import info.maaskant.wmsnotes.model.eventstore.DelayingEventStore
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.FileEventStore
import info.maaskant.wmsnotes.model.eventstore.InMemoryEventStore
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.model.projection.cache.*
import info.maaskant.wmsnotes.utilities.serialization.Serializer
import java.io.File
import javax.inject.Singleton

@Module
class ModelModule {

    @Provides
    fun eventSerializer(kryoPool: Pool<Kryo>): Serializer<Event> = KryoEventSerializer(kryoPool)

    @Singleton
    @Provides
    fun eventStore(eventSerializer: Serializer<Event>): EventStore {
        val realStore = if (storeInMemory) {
            InMemoryEventStore()
        } else {
            FileEventStore(File("desktop_data/events"), eventSerializer)
        }
        return if (delay) {
            DelayingEventStore(realStore)
        } else {
            realStore
        }
    }

    @Singleton
    @Provides
    fun noteCache(noteSerializer: Serializer<Note>): NoteCache =
            if (cache) {
                FileNoteCache(File("desktop_data/cache/projected_notes"), noteSerializer)
            } else {
                NoopNoteCache
            }

    @Singleton
    @Provides
    fun noteProjector(cachingNoteProjector: CachingNoteProjector): NoteProjector = cachingNoteProjector

    @Provides
    fun noteSerializer(kryoPool: Pool<Kryo>): Serializer<Note> = KryoNoteSerializer(kryoPool)

}