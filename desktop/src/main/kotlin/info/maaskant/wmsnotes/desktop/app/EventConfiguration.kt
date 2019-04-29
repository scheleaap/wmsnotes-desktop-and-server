package info.maaskant.wmsnotes.desktop.app

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.desktop.settings.Configuration.cache
import info.maaskant.wmsnotes.desktop.settings.Configuration.delay
import info.maaskant.wmsnotes.desktop.settings.Configuration.storeInMemory
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.KryoEventSerializer
import info.maaskant.wmsnotes.model.aggregaterepository.*
import info.maaskant.wmsnotes.model.eventstore.DelayingEventStore
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.FileEventStore
import info.maaskant.wmsnotes.model.eventstore.InMemoryEventStore
import info.maaskant.wmsnotes.model.folder.Folder
import info.maaskant.wmsnotes.model.folder.KryoFolderSerializer
import info.maaskant.wmsnotes.model.note.KryoNoteSerializer
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.utilities.serialization.Serializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import javax.inject.Singleton

@Configuration
class EventConfiguration {
    @Bean
    @Singleton
    fun eventSerializer(kryoPool: Pool<Kryo>): Serializer<Event> = KryoEventSerializer(kryoPool)

    @Bean
    @Singleton
    fun eventStore(@OtherConfiguration.AppDirectory appDirectory: File, eventSerializer: Serializer<Event>): EventStore {
        val realStore = if (storeInMemory) {
            InMemoryEventStore()
        } else {
            FileEventStore(appDirectory.resolve("events"), eventSerializer)
        }
        return if (delay) {
            DelayingEventStore(realStore)
        } else {
            realStore
        }
    }
}