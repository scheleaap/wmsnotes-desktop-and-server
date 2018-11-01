package info.maaskant.wmsnotes.desktop.app

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import dagger.Module
import dagger.Provides
import info.maaskant.wmsnotes.client.api.GrpcCommandMapper
import info.maaskant.wmsnotes.client.api.GrpcEventMapper
import info.maaskant.wmsnotes.client.synchronization.*
import info.maaskant.wmsnotes.client.synchronization.eventrepository.FileModifiableEventRepository
import info.maaskant.wmsnotes.client.synchronization.eventrepository.InMemoryModifiableEventRepository
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.desktop.app.Configuration.storeInMemory
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import info.maaskant.wmsnotes.utilities.persistence.FileStateRepository
import info.maaskant.wmsnotes.utilities.persistence.StateRepository
import info.maaskant.wmsnotes.utilities.serialization.Serializer
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Suppress("ConstantConditionIf")
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
    @ForLocalEvents
    fun localEventRepository(eventSerializer: Serializer<Event>) =
            if (storeInMemory) {
                InMemoryModifiableEventRepository()
            } else {
                FileModifiableEventRepository(File("desktop_data/synchronization/local_events"), eventSerializer)
            }

    @Singleton
    @Provides
    @ForRemoteEvents
    fun remoteEventRepository(eventSerializer: Serializer<Event>) =
            if (storeInMemory) {
                InMemoryModifiableEventRepository()
            } else {
                FileModifiableEventRepository(File("desktop_data/synchronization/remote_events"), eventSerializer)
            }

    @Singleton
    @Provides
    fun eventImporterStateSerializer(kryoPool: Pool<Kryo>): Serializer<EventImporterState> =
            KryoEventImporterStateSerializer(kryoPool)

    @Singleton
    @Provides
    @ForLocalEvents
    fun localEventImporterStateRepository(serializer: Serializer<EventImporterState>): StateRepository<EventImporterState> =
            FileStateRepository(
                    serializer = serializer,
                    file = File("desktop_data/synchronization/local_events/.state"),
                    scheduler = Schedulers.io(),
                    timeout = 1,
                    unit = TimeUnit.SECONDS
            )

    @Singleton
    @Provides
    @ForRemoteEvents
    fun remoteEventImporterStateRepository(serializer: Serializer<EventImporterState>): StateRepository<EventImporterState> =
            FileStateRepository(
                    serializer = serializer,
                    file = File("desktop_data/synchronization/remote_events/.state"),
                    scheduler = Schedulers.io(),
                    timeout = 1,
                    unit = TimeUnit.SECONDS
            )

    @Singleton
    @Provides
    fun localEventImporter(
            eventStore: EventStore,
            @ForLocalEvents eventRepository: ModifiableEventRepository,
            @ForLocalEvents stateRepository: StateRepository<EventImporterState>
    ) =
            LocalEventImporter(
                    eventStore,
                    eventRepository,
                    stateRepository.load()
            ).apply {
                stateRepository.connect(this)
            }

    @Singleton
    @Provides
    fun remoteEventImporter(
            grpcEventService: EventServiceGrpc.EventServiceBlockingStub,
            grpcEventMapper: GrpcEventMapper,
            @ForRemoteEvents eventRepository: ModifiableEventRepository,
            @ForLocalEvents stateRepository: StateRepository<EventImporterState>
    ) =
            RemoteEventImporter(
                    grpcEventService,
                    eventRepository,
                    grpcEventMapper,
                    stateRepository.load()
            ).apply {
                stateRepository.connect(this)
            }

    @Singleton
    @Provides
    fun synchronizerStateRepository(kryoPool: Pool<Kryo>): StateRepository<SynchronizerState> =
            FileStateRepository(
                    serializer = KryoSynchronizerStateSerializer(kryoPool),
                    file = File("desktop_data/synchronization/synchronizer.state"),
                    scheduler = Schedulers.io(),
                    timeout = 1,
                    unit = TimeUnit.SECONDS
            )

    @Singleton
    @Provides
    fun synchronizer(
            @ForLocalEvents localEvents: ModifiableEventRepository,
            @ForRemoteEvents remoteEvents: ModifiableEventRepository,
            remoteCommandService: CommandServiceGrpc.CommandServiceBlockingStub,
            eventToCommandMapper: EventToCommandMapper,
            grpcCommandMapper: GrpcCommandMapper,
            commandProcessor: CommandProcessor,
            noteProjector: NoteProjector,
            stateRepository: StateRepository<SynchronizerState>
    ) = Synchronizer(
            localEvents,
            remoteEvents,
            remoteCommandService,
            eventToCommandMapper,
            grpcCommandMapper,
            commandProcessor,
            noteProjector,
            stateRepository.load()
    ).apply {
        stateRepository.connect(this)
    }

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class ForSynchronization

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class ForLocalEvents

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class ForRemoteEvents

}