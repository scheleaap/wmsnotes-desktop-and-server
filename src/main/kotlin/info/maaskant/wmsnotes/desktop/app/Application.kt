package info.maaskant.wmsnotes.desktop.app

import info.maaskant.wmsnotes.desktop.view.MainView
import info.maaskant.wmsnotes.model.synchronization.InboundSynchronizer
import javafx.scene.image.Image
import javafx.stage.Stage
import org.kodein.di.generic.instance
import tornadofx.*

class Application : App(MainView::class, Styles::class) {

    private val inboundSynchronizer: InboundSynchronizer by kodein.instance()

    init {
        addStageIcon(Image("app-icon.png"))
    }

    override fun start(stage: Stage) {
        super.start(stage)
        inboundSynchronizer.start()
    }

    override fun stop() {
        super.stop()
        inboundSynchronizer.stop()
    }
}
