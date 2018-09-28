package info.maaskant.wmsnotes.desktop.app

import info.maaskant.wmsnotes.desktop.view.MainView
import info.maaskant.wmsnotes.model.synchronization.InboundSynchronizer
import javafx.scene.image.Image
import javafx.stage.Stage
import tornadofx.*

class Application : App(MainView::class, Styles::class) {

    private val inboundSynchronizer: InboundSynchronizer = Injector.instance.inboundSynchronizer()
    private val remoteEventImporter = Injector.instance.remoteEventImporter()

    init {
        addStageIcon(Image("app-icon.png"))

//        val a: ApplicationGraph = ApplicationGraph.init()
//        println(a.eventSerializer())
//        println(a.eventStore())
//        val b = object: DIContainer {
//            override fun <T : Any> getInstance(type: KClass<T>): T {
//                return a.inject(type)
//            }
//        }

    }

    override fun start(stage: Stage) {
        super.start(stage)
//        inboundSynchronizer.start()
//        remoteEventImporter.start()
    }

    override fun stop() {
        super.stop()
//        inboundSynchronizer.stop()
//        remoteEventImporter.stop()
    }
}
