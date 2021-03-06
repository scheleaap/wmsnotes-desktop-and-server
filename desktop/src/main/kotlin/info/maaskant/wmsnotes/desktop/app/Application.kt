package info.maaskant.wmsnotes.desktop.app

import info.maaskant.wmsnotes.desktop.design.Styles
import info.maaskant.wmsnotes.desktop.main.MainView
import info.maaskant.wmsnotes.desktop.main.NavigationViewModel
import javafx.scene.image.Image
import javafx.stage.Stage
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext
import tornadofx.*
import kotlin.reflect.KClass

@SpringBootApplication(scanBasePackages = ["info.maaskant.wmsnotes.desktop", "info.maaskant.wmsnotes.desktop"])
class Application : App(MainView::class, Styles::class) {

    private lateinit var context: ConfigurableApplicationContext

    private val navigationViewModel: NavigationViewModel by lazy {
        context.beanFactory.getBean(NavigationViewModel::class.java)
    }

    private val serviceManager: ApplicationServiceManager by lazy {
        context.beanFactory.getBean(ApplicationServiceManager::class.java)
    }

    override fun init() {
        context = SpringApplication.run(this.javaClass)
        context.autowireCapableBeanFactory.autowireBean(this)
        FX.dicontainer = object : DIContainer {
            override fun <T : Any> getInstance(type: KClass<T>): T = context.getBean(type.java)
        }

        addStageIcon(Image(javaClass.getResource("app-icon.png").toExternalForm()))
    }

    override fun start(stage: Stage) {
        super.start(stage)
        serviceManager.start()
        navigationViewModel.start()
    }

    override fun stop() {
        super.stop()
        serviceManager.shutdown()
        context.close()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Application::class.java, *args)
        }
    }
}
