package info.maaskant.wmsnotes.desktop.app

import info.maaskant.wmsnotes.model.CommandBus
import info.maaskant.wmsnotes.model.CommandExecution
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.folder.Folder
import info.maaskant.wmsnotes.model.folder.FolderCommandExecutor
import info.maaskant.wmsnotes.model.folder.FolderCommandToEventMapper
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.model.note.NoteCommandExecutor
import info.maaskant.wmsnotes.model.note.NoteCommandToEventMapper
import info.maaskant.wmsnotes.model.note.policy.NoteTitlePolicy
import info.maaskant.wmsnotes.model.note.policy.extractTitleFromContent
import io.reactivex.schedulers.Schedulers
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Configuration
class CommandConfiguration {
    @Bean
    @Singleton
    fun commandExecutionTimeout() = CommandExecution.Duration(500, TimeUnit.MILLISECONDS)

    @Bean
    @Singleton
    fun commandBus() = CommandBus()

    @Bean
    @Singleton
    fun folderCommandExecutor(
            commandBus: CommandBus,
            eventStore: EventStore,
            repository: AggregateRepository<Folder>
    ) = FolderCommandExecutor(
            commandBus,
            eventStore,
            repository,
            FolderCommandToEventMapper(),
            Schedulers.io()
    )

    @Bean
    @Singleton
    fun noteCommandExecutor(
            commandBus: CommandBus,
            eventStore: EventStore,
            repository: AggregateRepository<Note>
    ) = NoteCommandExecutor(
            commandBus,
            eventStore,
            repository,
            NoteCommandToEventMapper(),
            Schedulers.io()
    )

    @Bean
    @Singleton
    fun noteTitlePolicy(
            commandBus: CommandBus,
            eventStore: EventStore
    ) = NoteTitlePolicy(
            commandBus,
            eventStore,
            Schedulers.computation(),
            titleExtractor = ::extractTitleFromContent
    )
}