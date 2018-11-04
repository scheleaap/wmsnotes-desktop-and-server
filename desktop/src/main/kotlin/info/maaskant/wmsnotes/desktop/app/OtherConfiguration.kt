package info.maaskant.wmsnotes.desktop.app

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Configuration
class OtherConfiguration {

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class AppDirectory

    @Bean
    @Singleton
    @AppDirectory
    fun appDirectory(): File {
        return File("desktop_data")
    }

    @Bean
    @Singleton
    fun kryoPool(): Pool<Kryo> {
        return object : Pool<Kryo>(true, true) {
            override fun create(): Kryo = Kryo()
        }
    }

}