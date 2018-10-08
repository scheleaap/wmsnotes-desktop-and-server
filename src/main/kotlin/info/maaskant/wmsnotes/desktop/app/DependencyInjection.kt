package info.maaskant.wmsnotes.desktop.app

import dagger.Component
import dagger.Module
import dagger.Provides
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.client.synchronization.eventrepository.FileEventRepository
import info.maaskant.wmsnotes.model.serialization.EventSerializer
import info.maaskant.wmsnotes.model.serialization.KryoEventSerializer
import info.maaskant.wmsnotes.client.synchronization.MapDbImporterStateStorage
import info.maaskant.wmsnotes.client.synchronization.RemoteEventImporter
import info.maaskant.wmsnotes.model.eventstore.DatabaseEventStore
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.mapdb.DB
import org.mapdb.DBMaker
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
    fun commandProcessor(): CommandProcessor
    fun remoteEventImporter(): RemoteEventImporter
}


@Module
class ApplicationModule {

    @Provides
    fun eventSerializer(kryoEventSerializer: KryoEventSerializer): EventSerializer = kryoEventSerializer

    @Provides
    fun eventStore(eventSerializer: EventSerializer): EventStore = DatabaseEventStore(eventSerializer)

    @Provides
    fun mapDbDatabase(): DB {
        return DBMaker
                .fileDB("database.mapdb")
                .fileMmapEnableIfSupported()
                .closeOnJvmShutdown()
                .make()
    }

    @Provides
    fun remoteEventImporter(
            eventService: EventServiceGrpc.EventServiceBlockingStub,
            eventSerializer: EventSerializer,
            database: DB
    ) =
            RemoteEventImporter(
                    eventService,
                    FileEventRepository(File("importedRemoteEvents"), eventSerializer),
                    MapDbImporterStateStorage(MapDbImporterStateStorage.ImporterType.REMOTE, database)
            )

    @Provides
    fun eventService(managedChannel: ManagedChannel) =
            EventServiceGrpc.newBlockingStub(managedChannel)!!

    @Provides
    fun managedChannel(): ManagedChannel =
            ManagedChannelBuilder
                    .forAddress("localhost", 6565)
                    // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                    // needing certificates.
                    .usePlaintext()
                    .build()
}

