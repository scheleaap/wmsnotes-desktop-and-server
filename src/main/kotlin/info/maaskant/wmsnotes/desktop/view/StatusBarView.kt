package info.maaskant.wmsnotes.desktop.view

import info.maaskant.wmsnotes.desktop.app.Injector
import info.maaskant.wmsnotes.desktop.model.ApplicationModel
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import tornadofx.*
import tornadofx.controlsfx.statusbar

class StatusBarView : View() {

    private val applicationModel: ApplicationModel = Injector.instance.applicationModel()

    override val root = statusbar {}

    init {
        applicationModel
                .selectedNoteUpdates
                .observeOn(JavaFxScheduler.platform())
                .subscribe {
                    root.text = if (it.isPresent) "Note ${it.value?.title}" else null
                }

    }
}
