package info.maaskant.wmsnotes.desktop.app

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.desktop.settings.Configuration.cache
import info.maaskant.wmsnotes.desktop.settings.Configuration.delay
import info.maaskant.wmsnotes.desktop.settings.Configuration.storeInMemory
import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.aggregaterepository.*
import info.maaskant.wmsnotes.model.eventstore.DelayingEventStore
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.FileEventStore
import info.maaskant.wmsnotes.model.eventstore.InMemoryEventStore
import info.maaskant.wmsnotes.model.note.KryoNoteSerializer
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.utilities.serialization.Serializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import javax.inject.Singleton

@Suppress("ConstantConditionIf")
@Configuration
class ModelConfiguration {

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

    @Bean
    @Singleton
    fun noteCommandHandler(repository: AggregateRepository<Note>): AggregateCommandHandler<Note> =
            AggregateCommandHandler(
                    repository,
                    CommandToEventMapper()
            )

    @Bean
    @Singleton
    fun commandProcessor(eventStore: EventStore, commandHandler: AggregateCommandHandler<Note>): CommandProcessor =
            CommandProcessor(
                    eventStore,
                    commandHandler
            )

    @Bean
    @Singleton
    fun noteCache(@OtherConfiguration.AppDirectory appDirectory: File, serializer: Serializer<Note>): AggregateCache<Note> =
            if (cache && !storeInMemory) {
                FileAggregateCache(appDirectory.resolve("cache").resolve("projected_notes"), serializer)
            } else {
                NoopAggregateCache()
            }

    @Bean
    @Singleton
    fun noteRepository(eventStore: EventStore, cache: AggregateCache<Note>): AggregateRepository<Note> =
            CachingAggregateRepository(eventStore, cache, Note())

    @Bean
    @Singleton
    fun noteSerializer(kryoPool: Pool<Kryo>): Serializer<Note> = KryoNoteSerializer(kryoPool)

}