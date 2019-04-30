package info.maaskant.wmsnotes.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import javax.inject.Inject
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener


@SpringBootApplication(scanBasePackages = ["info.maaskant.wmsnotes.server"])
class Application @Inject constructor(
        private val applicationServiceManager: ApplicationServiceManager
) {
    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        applicationServiceManager.start()
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}