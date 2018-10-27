package info.maaskant.wmsnotes.server

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.CommandToEventMapper
import info.maaskant.wmsnotes.model.KryoEventSerializer
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.FileEventStore
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.model.projection.cache.CachingNoteProjector
import info.maaskant.wmsnotes.model.projection.cache.FileNoteCache
import info.maaskant.wmsnotes.model.projection.cache.KryoNoteSerializer
import info.maaskant.wmsnotes.model.projection.cache.NoteCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class ModelConfiguration {

    @Bean
    fun eventStore(kryoPool: Pool<Kryo>): EventStore =
            FileEventStore(
                    File("server_data/events"),
                    KryoEventSerializer(kryoPool)
            )

    @Bean
    fun model(eventStore: EventStore, noteProjector: NoteProjector): CommandProcessor =
            CommandProcessor(
                    eventStore,
                    noteProjector,
                    CommandToEventMapper()
            )

    @Bean
    fun noteCache(kryoPool: Pool<Kryo>): NoteCache = FileNoteCache(
            File("desktop_data/cache/projected_notes"),
            KryoNoteSerializer(kryoPool)
    )

    @Bean
    fun noteProjector(eventStore: EventStore, noteCache: NoteCache): NoteProjector =
            CachingNoteProjector(
                    eventStore,
                    noteCache
            )

}