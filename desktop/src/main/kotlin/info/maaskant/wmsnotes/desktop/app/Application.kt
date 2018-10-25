package info.maaskant.wmsnotes.desktop.app

import info.maaskant.wmsnotes.desktop.view.MainView
import info.maaskant.wmsnotes.desktop.view.Styles
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javafx.scene.image.Image
import javafx.stage.Stage
import tornadofx.*
import java.util.concurrent.TimeUnit

class Application : App(MainView::class, Styles::class) {

    private val applicationModel = Injector.instance.applicationModel()
    private val localEventImporter = Injector.instance.localEventImporter()
    private val remoteEventImporter = Injector.instance.remoteEventImporter()
    private val synchronizer = Injector.instance.synchronizer()

    private var timerDisposable: Disposable? = null

    init {
//        addStageIcon(Image("app-icon.png"))
    }

    override fun start(stage: Stage) {
        super.start(stage)
        applicationModel.start()
        if (timerDisposable == null) {
            timerDisposable = Observable
                    .interval(0, 10, TimeUnit.SECONDS)
                    .observeOn(Schedulers.io())
                    .subscribe {
                        synchronize()
                    }
        }
    }

    override fun stop() {
        super.stop()
        timerDisposable?.dispose()
    }

    private fun synchronize() {
        localEventImporter.loadAndStoreLocalEvents()
        remoteEventImporter.loadAndStoreRemoteEvents()
        synchronizer.synchronize()
    }

}
