package info.maaskant.wmsnotes.desktop.app;

import info.maaskant.wmsnotes.client.synchronization.SynchronizationTask
import info.maaskant.wmsnotes.desktop.main.NavigationViewModel
import info.maaskant.wmsnotes.model.AbstractCommandExecutor.Companion.connectToBus
import info.maaskant.wmsnotes.model.CommandBus
import info.maaskant.wmsnotes.model.folder.FolderCommandExecutor
import info.maaskant.wmsnotes.model.note.NoteCommandExecutor
import info.maaskant.wmsnotes.model.note.policy.NoteTitlePolicy
import io.reactivex.schedulers.Schedulers
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import javax.inject.Inject
import javax.inject.Singleton

@Service
@Singleton
class ApplicationServices @Inject constructor(
        private val commandBus: CommandBus,
        private val folderCommandExecutor: FolderCommandExecutor,
        private val synchronizationTask: SynchronizationTask,
        private val navigationViewModel: NavigationViewModel,
        private val noteCommandExecutor: NoteCommandExecutor,
        private val noteTitlePolicy: NoteTitlePolicy
) {
    fun start() {
        navigationViewModel.start()
        synchronizationTask.pause()
        synchronizationTask.start()

        connectToBus(folderCommandExecutor, commandBus, Schedulers.io())
        connectToBus(noteCommandExecutor, commandBus, Schedulers.io())
        noteTitlePolicy.start()
    }

    fun stop() {
        synchronizationTask.shutdown()
    }
}
