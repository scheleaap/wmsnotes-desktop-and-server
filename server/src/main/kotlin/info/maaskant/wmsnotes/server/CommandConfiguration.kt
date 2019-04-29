package info.maaskant.wmsnotes.server

import info.maaskant.wmsnotes.model.AbstractCommandExecutor
import info.maaskant.wmsnotes.model.AbstractCommandExecutor.Companion.connectToBus
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
    fun commandBusWithConnectedExecutors(
            folderCommandExecutor: FolderCommandExecutor,
            noteCommandExecutor: NoteCommandExecutor
    ): CommandBus {
        val commandBus = CommandBus()
        connectToBus(folderCommandExecutor, commandBus, Schedulers.io())
        connectToBus(noteCommandExecutor, commandBus, Schedulers.io())
        return commandBus
    }

    @Bean
    @Singleton
    fun folderCommandExecutor(
            eventStore: EventStore,
            repository: AggregateRepository<Folder>
    ) = FolderCommandExecutor(
            eventStore,
            repository,
            FolderCommandToEventMapper()
    )

    @Bean
    @Singleton
    fun noteCommandExecutor(
            eventStore: EventStore,
            repository: AggregateRepository<Note>
    ) = NoteCommandExecutor(
            eventStore,
            repository,
            NoteCommandToEventMapper()
    )
}