package info.maaskant.wmsnotes.desktop.app

import info.maaskant.wmsnotes.desktop.view.MainView
import info.maaskant.wmsnotes.desktop.view.Styles
import io.reactivex.disposables.Disposable
import javafx.stage.Stage
import tornadofx.*

class Application : App(MainView::class, Styles::class) {

    private val applicationModel = Injector.instance.applicationModel()
    private val synchronizationTask = Injector.instance.synchronizationTask()

    private var timerDisposable: Disposable? = null

    init {
//        addStageIcon(Image("app-icon.png"))
    }

    override fun start(stage: Stage) {
        super.start(stage)
        applicationModel.start()
        synchronizationTask.start()
    }

    override fun stop() {
        super.stop()
        synchronizationTask.shutdown()
    }

}
