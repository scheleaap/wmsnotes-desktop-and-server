package info.maaskant.wmsnotes.desktop.app

import info.maaskant.wmsnotes.client.synchronization.SynchronizationTask
import info.maaskant.wmsnotes.desktop.model.ApplicationModel
import info.maaskant.wmsnotes.desktop.view.MainView
import info.maaskant.wmsnotes.desktop.view.Styles
import javafx.stage.Stage
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext
import tornadofx.*
import kotlin.reflect.KClass

@SpringBootApplication(scanBasePackages = ["info.maaskant.wmsnotes.desktop", "info.maaskant.wmsnotes.desktop"])
class Application : App(MainView::class, Styles::class) {

    private lateinit var context: ConfigurableApplicationContext

    private val applicationModel: ApplicationModel by lazy {
        context.beanFactory.getBean(ApplicationModel::class.java)
    }
    private val synchronizationTask: SynchronizationTask by lazy {
        context.beanFactory.getBean(SynchronizationTask::class.java)
    }

    init {
//        addStageIcon(Image("app-icon.png"))
    }

    override fun init() {
        context = SpringApplication.run(this.javaClass)
        context.autowireCapableBeanFactory.autowireBean(this)
        FX.dicontainer = object : DIContainer {
            override fun <T : Any> getInstance(type: KClass<T>): T = context.getBean(type.java)
        }
    }

    override fun start(stage: Stage) {
        super.start(stage)
        applicationModel.start()
        synchronizationTask.pause()
        synchronizationTask.start()
    }

    override fun stop() {
        super.stop()
        synchronizationTask.shutdown()
        context.close()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            javafx.application.Application.launch(Application::class.java, *args)
        }
    }

}
