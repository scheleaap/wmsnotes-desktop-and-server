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
class OtherConfiguration {
    @Bean
    fun kryoPool(): Pool<Kryo> {
        return object : Pool<Kryo>(true, true) {
            override fun create(): Kryo = Kryo()
        }
    }
}