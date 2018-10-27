package info.maaskant.wmsnotes.desktop.app

import dagger.Module
import dagger.Provides
import info.maaskant.wmsnotes.desktop.app.Configuration.delay
import info.maaskant.wmsnotes.desktop.app.Configuration.storeInMemory
import info.maaskant.wmsnotes.model.eventstore.DelayingEventStore
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.FileEventStore
import info.maaskant.wmsnotes.model.eventstore.InMemoryEventStore
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.model.projection.cache.*
import info.maaskant.wmsnotes.utilities.serialization.EventSerializer
import info.maaskant.wmsnotes.utilities.serialization.KryoEventSerializer
import java.io.File
import javax.inject.Singleton

@Module
class ModelModule {

    @Provides
    fun eventSerializer(kryoEventSerializer: KryoEventSerializer): EventSerializer = kryoEventSerializer

    @Singleton
    @Provides
    fun eventStore(eventSerializer: EventSerializer): EventStore {
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
    fun noteCache(noteSerializer: NoteSerializer): NoteCache =
            FileNoteCache(File("desktop_data/cache/projected_notes"), noteSerializer)
    //    fun noteCache(): NoteCache = NoopNoteCache

    @Singleton
    @Provides
    fun noteProjector(cachingNoteProjector: CachingNoteProjector): NoteProjector = cachingNoteProjector

    @Provides
    fun noteSerializer(kryoNoteSerializer: KryoNoteSerializer): NoteSerializer = kryoNoteSerializer

}