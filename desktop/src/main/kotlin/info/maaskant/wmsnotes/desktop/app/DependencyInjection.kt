package info.maaskant.wmsnotes.desktop.app

import dagger.Component
import dagger.Module
import dagger.Provides
import info.maaskant.wmsnotes.client.api.GrpcCommandMapper
import info.maaskant.wmsnotes.client.indexing.NoteIndex
import info.maaskant.wmsnotes.client.synchronization.eventrepository.FileEventRepository
import info.maaskant.wmsnotes.client.synchronization.eventrepository.InMemoryEventRepository
import info.maaskant.wmsnotes.desktop.model.ApplicationModel
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.eventstore.DelayingEventStore
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.FileEventStore
import info.maaskant.wmsnotes.model.eventstore.InMemoryEventStore
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.model.projection.cache.CachingNoteProjector
import info.maaskant.wmsnotes.model.projection.cache.*
import info.maaskant.wmsnotes.client.api.GrpcEventMapper
import info.maaskant.wmsnotes.client.synchronization.*
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import info.maaskant.wmsnotes.utilities.serialization.EventSerializer
import info.maaskant.wmsnotes.utilities.serialization.KryoEventSerializer
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.reactivex.schedulers.Schedulers
import org.mapdb.DB
import org.mapdb.DBMaker
import java.io.File
import javax.inject.Singleton
import javax.inject.Qualifier


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
    fun localEventImporter(): LocalEventImporter
    fun remoteEventImporter(): RemoteEventImporter
    fun synchronizer(): Synchronizer
}


@Module
class ApplicationModule {

    private var storeInMemory =
//            true
            false

    private val delay =
//            true
            false

    @Provides
    fun eventSerializer(kryoEventSerializer: KryoEventSerializer): EventSerializer = kryoEventSerializer

    @Singleton
    @Provides
    fun grpcCommandService(managedChannel: ManagedChannel) =
            CommandServiceGrpc.newBlockingStub(managedChannel)!!

    @Singleton
    @Provides
    fun grpcEventService(managedChannel: ManagedChannel) =
            EventServiceGrpc.newBlockingStub(managedChannel)!!

    @Singleton
    @Provides
    fun eventStore(eventSerializer: EventSerializer): EventStore {
        val realStore = if (storeInMemory) {
            InMemoryEventStore()
        } else {
            FileEventStore(File("desktop_data/events"), eventSerializer)
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

    @Singleton
    @Provides
    @StateDatabase
    fun mapDbStateDatabase(): DB = if (storeInMemory) {
        DBMaker
                .memoryDB()
                .closeOnJvmShutdown()
                .make()
    } else {
        val file = File("desktop_data/synchronization/state.db")
        file.parentFile.mkdirs()
        DBMaker
                .fileDB(file)
                .fileMmapEnableIfSupported()
                .closeOnJvmShutdown()
                .make()
    }

    @Singleton
    @Provides
    @IndexDatabase
    fun mapDbIndexDatabase(): DB = if (storeInMemory) {
        DBMaker
                .memoryDB()
                .closeOnJvmShutdown()
                .make()
    } else {
        val file = File("desktop_data/indices.db")
        file.parentFile.mkdirs()
        DBMaker
                .fileDB(file)
                .fileMmapEnableIfSupported()
                .closeOnJvmShutdown()
                .make()
    }

    @Singleton
    @Provides
    fun noteCache(noteSerializer: NoteSerializer): NoteCache =
            FileNoteCache(File("desktop_data/cache/projected_notes"), noteSerializer)
    //    fun noteCache(): NoteCache = NoopNoteCache

    @Singleton
    @Provides
    fun noteIndex(eventStore: EventStore, @IndexDatabase database: DB): NoteIndex =
            NoteIndex(eventStore, database, Schedulers.io())

    @Singleton
    @Provides
    fun noteProjector(cachingNoteProjector: CachingNoteProjector): NoteProjector = cachingNoteProjector

    @Provides
    fun noteSerializer(kryoNoteSerializer: KryoNoteSerializer): NoteSerializer = kryoNoteSerializer

    @Singleton
    @Provides
    @LocalEventRepository
    fun modifiableEventRepositoryForLocalEvents(eventSerializer: EventSerializer) =
            if (storeInMemory) {
                InMemoryEventRepository()
            } else {
                FileEventRepository(File("desktop_data/synchronization/local_events"), eventSerializer)
            }

    @Singleton
    @Provides
    @RemoteEventRepository
    fun modifiableEventRepositoryForRemoteEvents(eventSerializer: EventSerializer) =
            if (storeInMemory) {
                InMemoryEventRepository()
            } else {
                FileEventRepository(File("desktop_data/synchronization/remote_events"), eventSerializer)
            }

    @Singleton
    @Provides
    fun localEventImporter(
            eventStore: EventStore,
            @LocalEventRepository eventRepository: ModifiableEventRepository,
            @StateDatabase database: DB
    ) =
            LocalEventImporter(
                    eventStore,
                    eventRepository,
                    MapDbImporterStateStorage(MapDbImporterStateStorage.ImporterType.LOCAL, database)
            )

    @Singleton
    @Provides
    fun remoteEventImporter(
            grpcEventService: EventServiceGrpc.EventServiceBlockingStub,
            @RemoteEventRepository eventRepository: ModifiableEventRepository,
            grpcEventMapper: GrpcEventMapper,
            @StateDatabase database: DB
    ) =
            RemoteEventImporter(
                    grpcEventService,
                    eventRepository,
                    grpcEventMapper,
                    MapDbImporterStateStorage(MapDbImporterStateStorage.ImporterType.REMOTE, database)
            )

    @Singleton
    @Provides
    fun synchronizer(
            @LocalEventRepository localEvents: ModifiableEventRepository,
            @RemoteEventRepository remoteEvents: ModifiableEventRepository,
            remoteCommandService: CommandServiceGrpc.CommandServiceBlockingStub,
            eventToCommandMapper: EventToCommandMapper,
            grpcCommandMapper: GrpcCommandMapper,
            commandProcessor: CommandProcessor,
            noteProjector: NoteProjector,
            @StateDatabase database: DB
    ) = Synchronizer(
            localEvents,
            remoteEvents,
            remoteCommandService,
            eventToCommandMapper,
            grpcCommandMapper,
            commandProcessor,
            noteProjector,
            MapDbSynchronizerStateStorage(database)
    )

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class StateDatabase

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class IndexDatabase

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class LocalEventRepository

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class RemoteEventRepository

}

