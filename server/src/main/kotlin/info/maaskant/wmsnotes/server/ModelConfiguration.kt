package info.maaskant.wmsnotes.server

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.AggregateCommandHandler
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.FileEventStore
import info.maaskant.wmsnotes.model.note.KryoNoteSerializer
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.note.NoteCommandToEventMapper
import info.maaskant.wmsnotes.model.note.KryoNoteEventSerializer
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateCache
import info.maaskant.wmsnotes.model.aggregaterepository.CachingAggregateRepository
import info.maaskant.wmsnotes.model.aggregaterepository.FileAggregateCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import javax.inject.Singleton

@Configuration
class ModelConfiguration {

    @Bean
    @Singleton
    fun eventStore(@OtherConfiguration.AppDirectory appDirectory: File, kryoPool: Pool<Kryo>): EventStore =
            FileEventStore(
                    appDirectory.resolve("events"),
                    KryoNoteEventSerializer(kryoPool)
            )

    @Bean
    @Singleton
    fun noteCommandHandler(repository: AggregateRepository<Note>): AggregateCommandHandler<Note> =
            AggregateCommandHandler(
                    repository,
                    NoteCommandToEventMapper()
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
    fun noteCache(@OtherConfiguration.AppDirectory appDirectory: File, kryoPool: Pool<Kryo>): AggregateCache<Note> =
            FileAggregateCache(
                    appDirectory.resolve("cache").resolve("projected_notes"),
                    KryoNoteSerializer(kryoPool)
            )

    @Bean
    @Singleton
    fun noteRepository(eventStore: EventStore, cache: AggregateCache<Note>): AggregateRepository<Note> =
            CachingAggregateRepository(eventStore, cache, Note())

}