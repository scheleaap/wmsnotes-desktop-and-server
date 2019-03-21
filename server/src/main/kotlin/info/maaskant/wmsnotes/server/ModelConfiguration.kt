package info.maaskant.wmsnotes.server

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.AggregateCommandHandler
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.KryoEventSerializer
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateCache
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.aggregaterepository.CachingAggregateRepository
import info.maaskant.wmsnotes.model.aggregaterepository.FileAggregateCache
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.FileEventStore
import info.maaskant.wmsnotes.model.folder.Folder
import info.maaskant.wmsnotes.model.folder.FolderCommand
import info.maaskant.wmsnotes.model.folder.FolderCommandToEventMapper
import info.maaskant.wmsnotes.model.folder.KryoFolderSerializer
import info.maaskant.wmsnotes.model.note.KryoNoteSerializer
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.model.note.NoteCommand
import info.maaskant.wmsnotes.model.note.NoteCommandToEventMapper
import info.maaskant.wmsnotes.utilities.serialization.Serializer
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
                    KryoEventSerializer(kryoPool)
            )

    @Bean
    @Singleton
    fun folderCommandHandler(repository: AggregateRepository<Folder>): AggregateCommandHandler<Folder> =
            AggregateCommandHandler(
                    FolderCommand::class,
                    repository,
                    FolderCommandToEventMapper()
            )

    @Bean
    @Singleton
    fun noteCommandHandler(repository: AggregateRepository<Note>): AggregateCommandHandler<Note> =
            AggregateCommandHandler(
                    NoteCommand::class,
                    repository,
                    NoteCommandToEventMapper()
            )

    @Bean
    @Singleton
    fun commandProcessor(eventStore: EventStore, folderCommandHandler: AggregateCommandHandler<Folder>, noteCommandHandler: AggregateCommandHandler<Note>): CommandProcessor =
            CommandProcessor(
                    eventStore,
                    folderCommandHandler,
                    noteCommandHandler
            )

    @Bean
    @Singleton
    fun folderCache(@OtherConfiguration.AppDirectory appDirectory: File, serializer: Serializer<Folder>): AggregateCache<Folder> =
            FileAggregateCache(appDirectory.resolve("cache").resolve("projected_folders"), serializer)

    @Bean
    @Singleton
    fun noteCache(@OtherConfiguration.AppDirectory appDirectory: File, serializer: Serializer<Note>): AggregateCache<Note> =
            FileAggregateCache(appDirectory.resolve("cache").resolve("projected_notes"), serializer)

    @Bean
    @Singleton
    fun folderRepository(eventStore: EventStore, cache: AggregateCache<Folder>): AggregateRepository<Folder> =
            CachingAggregateRepository(eventStore, cache, Folder())

    @Bean
    @Singleton
    fun noteRepository(eventStore: EventStore, cache: AggregateCache<Note>): AggregateRepository<Note> =
            CachingAggregateRepository(eventStore, cache, Note())

    @Bean
    @Singleton
    fun folderSerializer(kryoPool: Pool<Kryo>): Serializer<Folder> = KryoFolderSerializer(kryoPool)

    @Bean
    @Singleton
    fun noteSerializer(kryoPool: Pool<Kryo>): Serializer<Note> = KryoNoteSerializer(kryoPool)
}