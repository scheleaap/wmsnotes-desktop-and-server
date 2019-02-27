package info.maaskant.wmsnotes.desktop.app

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.desktop.client.indexing.KryoNoteIndexStateSerializer
import info.maaskant.wmsnotes.desktop.client.indexing.NoteIndex
import info.maaskant.wmsnotes.desktop.client.indexing.NoteIndexState
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
    fun noteIndexStateRepository(@OtherConfiguration.AppDirectory appDirectory: File, kryoPool: Pool<Kryo>): StateRepository<NoteIndexState> =
            FileStateRepository(
                    serializer = KryoNoteIndexStateSerializer(kryoPool),
                    file = appDirectory.resolve("cache").resolve("note_index"),
                    scheduler = Schedulers.io(),
                    timeout = 1,
                    unit = TimeUnit.SECONDS
            )

    @Bean
    @Singleton
    fun noteIndex(eventStore: EventStore, stateRepository: StateRepository<NoteIndexState>): NoteIndex {
        return NoteIndex(
                eventStore,
                stateRepository.load(),
                Schedulers.io()
        ).apply {
            stateRepository.connect(this)
        }
    }
}