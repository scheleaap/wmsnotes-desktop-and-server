package info.maaskant.wmsnotes.desktop.app

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.client.indexing.TreeIndex
import info.maaskant.wmsnotes.model.folder.FolderCommandExecutor
import info.maaskant.wmsnotes.model.note.NoteCommandExecutor
import info.maaskant.wmsnotes.model.note.policy.NoteTitlePolicy
import info.maaskant.wmsnotes.utilities.ApplicationService
import info.maaskant.wmsnotes.utilities.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Configuration
class OtherConfiguration {

    private val logger by logger()

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class AppDirectory

    @Bean
    @Singleton
    @AppDirectory
    fun appDirectory(@Value("\${rootDirectory:#{null}}") rootDirectory: String?): File {
        return if (rootDirectory != null) {
            File(rootDirectory)
        } else {
            if (System.getProperty("os.name").startsWith("Windows")) {
                File(System.getenv("APPDATA")).resolve("WMS Notes").resolve("Desktop")
            } else {
                File(System.getProperty("user.home")).resolve(".wmsnotes").resolve("desktop")
            }
        }.also {
            logger.info("Using application directory: $it")
        }
    }

    @Bean
    @Singleton
    fun applicationServices(
            folderCommandExecutor: FolderCommandExecutor,
            noteCommandExecutor: NoteCommandExecutor,
            noteTitlePolicy: NoteTitlePolicy,
            treeIndex: TreeIndex
    ): List<ApplicationService> = listOf(
            folderCommandExecutor,
            noteCommandExecutor,
            noteTitlePolicy,
            treeIndex
    )

    @Bean
    @Singleton
    fun kryoPool(): Pool<Kryo> {
        return object : Pool<Kryo>(true, true) {
            override fun create(): Kryo = Kryo()
        }
    }
}