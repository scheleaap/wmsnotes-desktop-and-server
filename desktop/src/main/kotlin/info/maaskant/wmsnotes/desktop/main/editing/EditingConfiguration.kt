package info.maaskant.wmsnotes.desktop.main.editing

import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.KeepType
import com.vladsch.flexmark.util.options.DataHolder
import com.vladsch.flexmark.util.options.MutableDataSet
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
    fun flexmarkOptions(): DataHolder =
            MutableDataSet()
                    .set(Parser.EXTENSIONS, listOf(
                            TablesExtension.create()
                    ))
                    // .set(Parser.REFERENCES_KEEP, KeepType.LAST)

                    .set(HtmlRenderer.INDENT_SIZE, 2)
                    .set(HtmlRenderer.PERCENT_ENCODE_URLS, true)

                    // For full GFM table compatibility add the following table extension options:
                    .set(TablesExtension.COLUMN_SPANS, false)
                    .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
                    .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
                    .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true)

    @Bean
    @Singleton
    fun parser(options: DataHolder): Parser =
            Parser.builder(options).build()

    @Bean
    @Singleton
    fun renderer(options: DataHolder): Renderer =
            FlexmarkPreviewRenderer(options)

    @Bean
    @Singleton
    @PreviewAttachmentStorage.PreviewAttachmentDirectory
    fun previewAttachmentDirectory(@OtherConfiguration.AppDirectory appDirectory: File) =
            appDirectory.resolve("cache").resolve("attachments")
}