package info.maaskant.wmsnotes.server

import info.maaskant.wmsnotes.model.CommandProcessor
import info.maaskant.wmsnotes.model.CommandToEventMapper
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.FileEventStore
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.model.projection.cache.*
import info.maaskant.wmsnotes.utilities.serialization.EventSerializer
import info.maaskant.wmsnotes.utilities.serialization.KryoEventSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class ModelConfiguration {

    @Bean
    fun eventStore(): EventStore =
            FileEventStore(
                    File("server_data/events"),
                    KryoEventSerializer()
            )

    @Bean
    fun model(eventStore: EventStore, noteProjector: NoteProjector): CommandProcessor =
            CommandProcessor(
                    eventStore,
                    noteProjector,
                    CommandToEventMapper()
            )

    @Bean
    fun noteCache(): NoteCache = FileNoteCache(
            File("desktop_data/cache/projected_notes"),
            KryoNoteSerializer()
    )


    @Bean
    fun noteProjector(eventStore: EventStore, noteCache: NoteCache): NoteProjector =
            CachingNoteProjector(
                    eventStore,
                    noteCache
            )

}