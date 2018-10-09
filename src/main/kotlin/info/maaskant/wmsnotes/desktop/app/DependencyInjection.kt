package info.maaskant.wmsnotes.desktop.app

import dagger.Component
import dagger.Module
import dagger.Provides
import info.maaskant.wmsnotes.client.synchronization.MapDbImporterStateStorage
import info.maaskant.wmsnotes.client.synchronization.RemoteEventImporter
import info.maaskant.wmsnotes.client.synchronization.eventrepository.FileEventRepository
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.FileEventStore
import info.maaskant.wmsnotes.model.projection.DefaultNoteProjector
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.utilities.serialization.EventSerializer
import info.maaskant.wmsnotes.utilities.serialization.KryoEventSerializer
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
    fun eventService(managedChannel: ManagedChannel) =
            EventServiceGrpc.newBlockingStub(managedChannel)!!

    @Provides
    fun eventStore(eventSerializer: EventSerializer): EventStore = FileEventStore(File("eventStore"), eventSerializer)

    @Provides
    fun managedChannel(): ManagedChannel =
            ManagedChannelBuilder
                    .forAddress("localhost", 6565)
                    // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                    // needing certificates.
                    .usePlaintext()
                    .build()

    @Provides
    fun mapDbDatabase(): DB {
        return DBMaker
                .fileDB("database.mapdb")
                .fileMmapEnableIfSupported()
                .closeOnJvmShutdown()
                .make()
    }

    @Provides
    fun noteProjector(): NoteProjector = DefaultNoteProjector()

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
}

