package info.maaskant.wmsnotes.server

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.KryoEventSerializer
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.FileEventStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import javax.inject.Singleton

@Configuration
class EventConfiguration {
    @Bean
    @Singleton
    fun eventStore(@OtherConfiguration.AppDirectory appDirectory: File, kryoPool: Pool<Kryo>): EventStore =
            FileEventStore(
                    appDirectory.resolve("events"),
                    KryoEventSerializer(kryoPool)
            )
}