package info.maaskant.wmsnotes.desktop.app

import dagger.Component
import dagger.Module
import dagger.Provides
import info.maaskant.wmsnotes.model.EventStore
import info.maaskant.wmsnotes.model.Model
import info.maaskant.wmsnotes.model.eventrepository.EventRepository
import info.maaskant.wmsnotes.model.eventrepository.FileEventRepository
import info.maaskant.wmsnotes.model.serialization.EventSerializer
import info.maaskant.wmsnotes.model.serialization.KryoEventSerializer
import info.maaskant.wmsnotes.model.synchronization.InboundSynchronizer
import info.maaskant.wmsnotes.model.synchronization.RemoteEventImporter
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.io.File
import javax.inject.Singleton

object Injector {
    val instance: ApplicationGraph = DaggerApplicationGraph.builder()
            .applicationModule(ApplicationModule())
            .build()
}

@Singleton
@Component(modules = [ApplicationModule::class])
interface ApplicationGraph {
    fun inboundSynchronizer(): InboundSynchronizer
    fun model(): Model
    fun remoteEventImporter(): RemoteEventImporter
}


@Module
class ApplicationModule {

    @Provides
    fun eventSerializer(kryoEventSerializer: KryoEventSerializer): EventSerializer = kryoEventSerializer

//    @Provides
//    fun eventRepository(fileEventRepository: FileEventRepository): EventRepository = fileEventRepository

    @Provides
    fun inboundSynchronizer(managedChannel: ManagedChannel, eventStore: EventStore, model: Model) =
            InboundSynchronizer(managedChannel, eventStore, model)

    @Provides
    fun remoteEventImporter(eventService: EventServiceGrpc.EventServiceBlockingStub, eventSerializer: EventSerializer) =
            RemoteEventImporter(eventService, FileEventRepository(File("importedRemoteEvents"), eventSerializer))

    @Provides
    fun eventService(managedChannel: ManagedChannel) =
            EventServiceGrpc.newBlockingStub(managedChannel)

    @Provides
    fun managedChannel(): ManagedChannel =
            ManagedChannelBuilder
                    .forAddress("localhost", 6565)
                    // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                    // needing certificates.
                    .usePlaintext()
                    .build()
}

