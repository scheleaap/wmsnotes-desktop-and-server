package info.maaskant.wmsnotes.desktop.app

import info.maaskant.wmsnotes.model.CommandBus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.inject.Singleton

@Suppress("ConstantConditionIf")
@Configuration
class CommandConfiguration {
    @Bean
    @Singleton
    fun commandBus(): CommandBus =
            CommandBus()
}