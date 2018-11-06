package info.maaskant.wmsnotes.desktop.main.editing

import info.maaskant.wmsnotes.desktop.main.editing.preview.FlexmarkPreviewRenderer
import info.maaskant.wmsnotes.desktop.main.editing.preview.Renderer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.inject.Singleton

@Configuration
class EditingConfiguration {
    @Bean
    @Singleton
    fun renderer(): Renderer = FlexmarkPreviewRenderer()
}