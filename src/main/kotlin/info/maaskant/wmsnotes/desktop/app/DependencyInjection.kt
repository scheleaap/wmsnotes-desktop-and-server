package info.maaskant.wmsnotes.desktop.app

import dagger.Component
import dagger.Module
import dagger.Provides
import info.maaskant.wmsnotes.client.synchronization.MapDbImporterStateStorage
import info.maaskant.wmsnotes.client.synchronization.RemoteEventImporter
import info.maaskant.wmsnotes.client.synchronization.eventrepository.FileEventRepository
import info.maaskant.wmsnotes.client.synchronization.eventrepository.InMemoryEventRepository
import info.maaskant.wmsnotes.desktop.model.ApplicationModel
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.eventstore.DelayingEventStore
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.FileEventStore
import info.maaskant.wmsnotes.model.eventstore.InMemoryEventStore
import info.maaskant.wmsnotes.model.projection.DefaultNoteProjector
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.server.api.GrpcEventMapper
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import info.maaskant.wmsnotes.utilities.serialization.EventSerializer
import info.maaskant.wmsnotes.utilities.serialization.KryoEventSerializer
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
    fun applicationModel(): ApplicationModel
    fun commandProcessor(): CommandProcessor
    fun remoteEventImporter(): RemoteEventImporter
}


@Module
class ApplicationModule {

    private var storeInMemory = true
//    private var storeInMemory = false

    private val delay = true
//    private val delay = false

    @Provides
    fun eventSerializer(kryoEventSerializer: KryoEventSerializer): EventSerializer = kryoEventSerializer

    @Singleton
    @Provides
    fun eventService(managedChannel: ManagedChannel) =
            EventServiceGrpc.newBlockingStub(managedChannel)!!

    @Singleton
    @Provides
    fun eventStore(eventSerializer: EventSerializer): EventStore {
        val realStore = if (storeInMemory) {
            InMemoryEventStore()
        } else {
            FileEventStore(File("data/events"), eventSerializer)
        }
        return if (delay) {
            DelayingEventStore(realStore)
        } else {
            realStore
        }
    }


    @Singleton
    @Provides
    fun managedChannel(): ManagedChannel =
            ManagedChannelBuilder
                    .forAddress("localhost", 6565)
                    // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                    // needing certificates.
                    .usePlaintext()
                    .build()

    // Qualify once we're using MapDB for indices as well
    @Singleton
    @Provides
    fun mapDbDatabase(): DB = if (storeInMemory) {
        DBMaker
                .memoryDB()
                .closeOnJvmShutdown()
                .make()
    } else {
        val file = File("data/synchronization/state.db")
        file.parentFile.mkdirs()
        DBMaker
                .fileDB(file)
                .fileMmapEnableIfSupported()
                .closeOnJvmShutdown()
                .make()
    }

    @Singleton
    @Provides
    fun noteProjector(eventStore: EventStore): NoteProjector = DefaultNoteProjector(eventStore)

    @Singleton
    @Provides
    fun remoteEventImporter(
            eventService: EventServiceGrpc.EventServiceBlockingStub,
            eventSerializer: EventSerializer,
            grpcEventMapper: GrpcEventMapper,
            database: DB
    ): RemoteEventImporter {
        val eventRepository = if (storeInMemory) {
            InMemoryEventRepository()
        } else {
            FileEventRepository(File("data/synchronization/remote_events"), eventSerializer)
        }
        val importerStateStorage = MapDbImporterStateStorage(MapDbImporterStateStorage.ImporterType.REMOTE, database)
        return RemoteEventImporter(eventService, eventRepository, grpcEventMapper, importerStateStorage)
    }
}

