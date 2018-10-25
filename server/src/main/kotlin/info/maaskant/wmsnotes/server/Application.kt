package info.maaskant.wmsnotes.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication(scanBasePackages = ["info.maaskant.wmsnotes.server"])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}