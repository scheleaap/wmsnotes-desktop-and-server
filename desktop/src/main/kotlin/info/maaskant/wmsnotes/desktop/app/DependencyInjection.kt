package info.maaskant.wmsnotes.desktop.app

import dagger.Component
import info.maaskant.wmsnotes.client.synchronization.LocalEventImporter
import info.maaskant.wmsnotes.client.synchronization.RemoteEventImporter
import info.maaskant.wmsnotes.client.synchronization.SynchronizationTask
import info.maaskant.wmsnotes.client.synchronization.Synchronizer
import info.maaskant.wmsnotes.desktop.model.ApplicationModel
import info.maaskant.wmsnotes.model.CommandProcessor
import javax.inject.Singleton

object Injector {
    val instance: ApplicationGraph = DaggerApplicationGraph.builder()
            .modelModule(ModelModule())
            .indexingModule(IndexingModule())
            .synchronizationModule(SynchronizationModule())
            .build()
}

@Singleton
@Component(modules = [ModelModule::class, IndexingModule::class, SynchronizationModule::class, OtherModule::class])
interface ApplicationGraph {
    fun applicationModel(): ApplicationModel
    fun commandProcessor(): CommandProcessor
    fun localEventImporter(): LocalEventImporter
    fun remoteEventImporter(): RemoteEventImporter
    fun synchronizationTask(): SynchronizationTask

}

object Configuration {
    const val storeInMemory =
//            true
            false

    const val delay =
//            true
            false

    const val cache =
            true
//            false
}
