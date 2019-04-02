package info.maaskant.wmsnotes.desktop.app

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.client.indexing.DefaultNodeSortingStrategy
import info.maaskant.wmsnotes.client.indexing.KryoTreeIndexStateSerializer
import info.maaskant.wmsnotes.client.indexing.TreeIndex
import info.maaskant.wmsnotes.client.indexing.TreeIndexState
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.utilities.persistence.FileStateRepository
import info.maaskant.wmsnotes.utilities.persistence.StateRepository
import io.reactivex.schedulers.Schedulers
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Configuration
class IndexingConfiguration {
    @Bean
    @Singleton
    fun treeIndexStateRepository(@OtherConfiguration.AppDirectory appDirectory: File, kryoPool: Pool<Kryo>): StateRepository<TreeIndexState> =
            FileStateRepository(
                    serializer = KryoTreeIndexStateSerializer(kryoPool),
                    file = appDirectory.resolve("cache").resolve("tree_index"),
                    scheduler = Schedulers.io(),
                    timeout = 1,
                    unit = TimeUnit.SECONDS
            )

    @Bean
    @Singleton
    fun treeIndex(eventStore: EventStore, stateRepository: StateRepository<TreeIndexState>): TreeIndex {
        return TreeIndex(
                eventStore,
                DefaultNodeSortingStrategy(),
                stateRepository.load(),
                Schedulers.io()
        ).apply {
            stateRepository.connect(this)
        }
    }
}