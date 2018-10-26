package info.maaskant.wmsnotes.desktop.app

import dagger.Module
import dagger.Provides
import info.maaskant.wmsnotes.client.api.GrpcCommandMapper
import info.maaskant.wmsnotes.client.api.GrpcEventMapper
import info.maaskant.wmsnotes.client.indexing.NoteIndex
import info.maaskant.wmsnotes.client.synchronization.*
import info.maaskant.wmsnotes.client.synchronization.eventrepository.FileModifiableEventRepository
import info.maaskant.wmsnotes.client.synchronization.eventrepository.InMemoryModifiableEventRepository
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.desktop.app.Configuration.storeInMemory
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.eventstore.DelayingEventStore
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.FileEventStore
import info.maaskant.wmsnotes.model.eventstore.InMemoryEventStore
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.model.projection.cache.*
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
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
class SynchronizationModule {

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
    fun managedChannel(): ManagedChannel =
            ManagedChannelBuilder.forAddress("localhost", 6565)
                    // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                    // needing certificates.
                    .usePlaintext()
                    .build()

    @Singleton
    @Provides
    @StateDatabase
    fun mapDbStateDatabase(): DB = if (storeInMemory) {
        DBMaker.memoryDB()
                .closeOnJvmShutdown()
                .make()
    } else {
        val file = File("desktop_data/synchronization/state.db")
        file.parentFile.mkdirs()
        DBMaker.fileDB(file)
                .fileMmapEnableIfSupported()
                .closeOnJvmShutdown()
                .make()
    }

    @Singleton
    @Provides
    @LocalEventRepository
    fun modifiableEventRepositoryForLocalEvents(eventSerializer: EventSerializer) =
            if (storeInMemory) {
                InMemoryModifiableEventRepository()
            } else {
                FileModifiableEventRepository(File("desktop_data/synchronization/local_events"), eventSerializer)
            }

    @Singleton
    @Provides
    @RemoteEventRepository
    fun modifiableEventRepositoryForRemoteEvents(eventSerializer: EventSerializer) =
            if (storeInMemory) {
                InMemoryModifiableEventRepository()
            } else {
                FileModifiableEventRepository(File("desktop_data/synchronization/remote_events"), eventSerializer)
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
    annotation class LocalEventRepository

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class RemoteEventRepository

}