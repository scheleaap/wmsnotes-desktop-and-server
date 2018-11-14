package info.maaskant.wmsnotes.desktop.main.editing

import info.maaskant.wmsnotes.desktop.app.OtherConfiguration
import info.maaskant.wmsnotes.desktop.main.editing.preview.FlexmarkPreviewRenderer
import info.maaskant.wmsnotes.desktop.main.editing.preview.PreviewAttachmentStorage
import info.maaskant.wmsnotes.desktop.main.editing.preview.Renderer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import javax.inject.Singleton

@Configuration
class EditingConfiguration {
    @Bean
    @Singleton
    fun renderer(): Renderer = FlexmarkPreviewRenderer()

    @Bean
    @Singleton
    @PreviewAttachmentStorage.PreviewAttachmentDirectory
    fun previewAttachmentDirectory(@OtherConfiguration.AppDirectory appDirectory: File) =
            appDirectory.resolve("cache").resolve("attachments")
}