package info.maaskant.wmsnotes.desktop.app

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.client.api.GrpcCommandMapper
import info.maaskant.wmsnotes.client.api.GrpcEventMapper
import info.maaskant.wmsnotes.client.synchronization.*
import info.maaskant.wmsnotes.client.synchronization.commandexecutor.LocalCommandExecutor
import info.maaskant.wmsnotes.client.synchronization.commandexecutor.RemoteCommandExecutor
import info.maaskant.wmsnotes.client.synchronization.eventrepository.FileModifiableEventRepository
import info.maaskant.wmsnotes.client.synchronization.eventrepository.InMemoryModifiableEventRepository
import info.maaskant.wmsnotes.client.synchronization.eventrepository.ModifiableEventRepository
import info.maaskant.wmsnotes.client.synchronization.strategy.LocalOnlySynchronizationStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.MultipleSynchronizationStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.RemoteOnlySynchronizationStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.SynchronizationStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.*
import info.maaskant.wmsnotes.desktop.settings.Configuration.storeInMemory
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.server.command.grpc.CommandServiceGrpc
import info.maaskant.wmsnotes.server.command.grpc.EventServiceGrpc
import info.maaskant.wmsnotes.utilities.persistence.FileStateRepository
import info.maaskant.wmsnotes.utilities.persistence.StateRepository
import info.maaskant.wmsnotes.utilities.serialization.Serializer
import io.grpc.Deadline
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.reactivex.schedulers.Schedulers
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Suppress("ConstantConditionIf")
@Configuration
class SynchronizationConfiguration {

    @Bean
    @Singleton
    fun synchronizer(
            @ForLocalEvents localEvents: ModifiableEventRepository,
            @ForRemoteEvents remoteEvents: ModifiableEventRepository,
            synchronizationStrategy: SynchronizationStrategy,
            eventToCommandMapper: EventToCommandMapper,
            localCommandExecutor: LocalCommandExecutor,
            remoteCommandExecutor: RemoteCommandExecutor,
            stateRepository: StateRepository<SynchronizerState>
    ) = NewSynchronizer(
            localEvents,
            remoteEvents,
            synchronizationStrategy,
            eventToCommandMapper,
            localCommandExecutor,
            remoteCommandExecutor,
            stateRepository.load()
    ).apply {
        stateRepository.connect(this)
    }

    @Bean
    @Singleton
    fun synchronizerStateRepository(@OtherConfiguration.AppDirectory appDirectory: File, kryoPool: Pool<Kryo>): StateRepository<SynchronizerState> =
            FileStateRepository(
                    serializer = KryoSynchronizerStateSerializer(kryoPool),
                    file = appDirectory.resolve("synchronization").resolve("synchronizer.state"),
                    scheduler = Schedulers.io(),
                    timeout = 1,
                    unit = TimeUnit.SECONDS
            )

    @Bean
    @Singleton
    fun synchronizationTask(localEventImporter: LocalEventImporter, remoteEventImporter: RemoteEventImporter, synchronizer: NewSynchronizer) =
            SynchronizationTask(localEventImporter, remoteEventImporter, synchronizer)

    @Bean
    @Singleton
    @ForLocalEvents
    fun localEventRepository(@OtherConfiguration.AppDirectory appDirectory: File, eventSerializer: Serializer<Event>) =
            if (storeInMemory) {
                InMemoryModifiableEventRepository()
            } else {
                FileModifiableEventRepository(appDirectory.resolve("synchronization").resolve("local_events"), eventSerializer)
            }

    @Bean
    @Singleton
    @ForRemoteEvents
    fun remoteEventRepository(@OtherConfiguration.AppDirectory appDirectory: File, eventSerializer: Serializer<Event>) =
            if (storeInMemory) {
                InMemoryModifiableEventRepository()
            } else {
                FileModifiableEventRepository(appDirectory.resolve("synchronization").resolve("remote_events"), eventSerializer)
            }

    @Bean
    @Singleton
    fun eventImporterStateSerializer(kryoPool: Pool<Kryo>): Serializer<EventImporterState> =
            KryoEventImporterStateSerializer(kryoPool)

    @Bean
    @Singleton
    @ForLocalEvents
    fun localEventImporterStateRepository(@OtherConfiguration.AppDirectory appDirectory: File, serializer: Serializer<EventImporterState>): StateRepository<EventImporterState> =
            FileStateRepository(
                    serializer = serializer,
                    file = appDirectory.resolve("synchronization").resolve("local_events").resolve(".state"),
                    scheduler = Schedulers.io(),
                    timeout = 1,
                    unit = TimeUnit.SECONDS
            )

    @Bean
    @Singleton
    @ForRemoteEvents
    fun remoteEventImporterStateRepository(@OtherConfiguration.AppDirectory appDirectory: File, serializer: Serializer<EventImporterState>): StateRepository<EventImporterState> =
            FileStateRepository(
                    serializer = serializer,
                    file = appDirectory.resolve("synchronization").resolve("remote_events").resolve(".state"),
                    scheduler = Schedulers.io(),
                    timeout = 1,
                    unit = TimeUnit.SECONDS
            )

    @Bean
    @Singleton
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

    @Bean
    @Singleton
    fun remoteEventImporter(
            grpcEventService: EventServiceGrpc.EventServiceBlockingStub,
            grpcDeadline: Deadline,
            grpcEventMapper: GrpcEventMapper,
            @ForRemoteEvents eventRepository: ModifiableEventRepository,
            @ForLocalEvents stateRepository: StateRepository<EventImporterState>
    ) =
            RemoteEventImporter(
                    grpcEventService,
                    grpcDeadline,
                    eventRepository,
                    grpcEventMapper,
                    stateRepository.load()
            ).apply {
                stateRepository.connect(this)
            }

    @Bean
    @Singleton
    fun synchronizationStrategy(mergeStrategy: MergeStrategy, noteProjector: NoteProjector) =
            MultipleSynchronizationStrategy(
                    LocalOnlySynchronizationStrategy(),
                    RemoteOnlySynchronizationStrategy(),
                    MergingSynchronizationStrategy(
                            mergeStrategy = mergeStrategy,
                            noteProjector = noteProjector
                    )

            )

    @Bean
    @Singleton
    fun mergeStrategy(differenceAnalyzer: DifferenceAnalyzer, differenceCompensator: DifferenceCompensator) =
            KeepBothMergeStrategy(differenceAnalyzer, differenceCompensator) { UUID.randomUUID().toString() }

    @Bean
    @Singleton
    fun differenceAnalyzer() = DifferenceAnalyzer()

    @Bean
    @Singleton
    fun differenceCompensator() = DifferenceCompensator()

    @Bean
    @Singleton
    fun eventToCommandMapper() = EventToCommandMapper()

    @Bean
    @Singleton
    fun localCommandExecutor(commandProcessor: CommandProcessor) =
            LocalCommandExecutor(commandProcessor)

    @Bean
    @Singleton
    fun remoteCommandExecutor(
            grpcCommandMapper: GrpcCommandMapper,
            grpcCommandService: CommandServiceGrpc.CommandServiceBlockingStub,
            grpcDeadline: Deadline
    ) =
            RemoteCommandExecutor(grpcCommandMapper, grpcCommandService, grpcDeadline)

    @Bean
    @Singleton
    @ServerHostname
    fun serverHostname(@Value("\${server.hostname:localhost}") hostname: String): String = hostname

    @Bean
    @Singleton
    fun grpcDeadline() = Deadline.after(1, TimeUnit.SECONDS)

    @Bean
    @Singleton
    fun grpcCommandService(managedChannel: ManagedChannel) =
            CommandServiceGrpc.newBlockingStub(managedChannel)!!

    @Bean
    @Singleton
    fun grpcEventService(managedChannel: ManagedChannel) =
            EventServiceGrpc.newBlockingStub(managedChannel)!!

    @Bean
    @Singleton
    fun managedChannel(@ServerHostname hostname: String): ManagedChannel =
            ManagedChannelBuilder.forAddress(hostname, 6565)
                    // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                    // needing certificates.
                    .usePlaintext() // TODO enable SSL/TLS
                    .build()

    @Bean
    @Singleton
    fun grpcCommandMapper() = GrpcCommandMapper()

    @Bean
    @Singleton
    fun grpcEventMapper() = GrpcEventMapper()


    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class ServerHostname

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class ForLocalEvents

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class ForRemoteEvents

}