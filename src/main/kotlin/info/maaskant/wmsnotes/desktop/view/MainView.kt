package info.maaskant.wmsnotes.desktop.view

import info.maaskant.wmsnotes.desktop.app.Injector
import info.maaskant.wmsnotes.desktop.model.ApplicationModel
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import javafx.geometry.Orientation
import javafx.scene.layout.BorderPane
import tornadofx.*

class MainView : View() {
    private val applicationTitle = "WMS Notes"

    override val root = BorderPane()

    private val applicationModel: ApplicationModel = Injector.instance.applicationModel()

    private val treeView: TreeView by inject()
    private val detailView: DetailView by inject()

    init {
        title = applicationTitle

        with(root) {
            setPrefSize(940.0, 610.0)
            top<ToolbarView>()
            center = splitpane {
                orientation = Orientation.HORIZONTAL
                setDividerPosition(0, 0.3)
                this += treeView
                this += detailView
            }
            bottom<StatusBarView>()
        }

        applicationModel
                .selectedNoteUpdates
                .observeOn(JavaFxScheduler.platform())
                .subscribe {
                    if (it.value != null) {
                        title = "$applicationTitle - ${it.value.title}"
                    } else {
                        title = applicationTitle
                    }
                }
    }
}