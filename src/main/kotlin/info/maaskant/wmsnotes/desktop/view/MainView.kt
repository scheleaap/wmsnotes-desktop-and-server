package info.maaskant.wmsnotes.desktop.view

import info.maaskant.wmsnotes.desktop.app.Injector
import info.maaskant.wmsnotes.desktop.model.ApplicationModel
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import javafx.geometry.Orientation
import javafx.scene.layout.BorderPane
import tornadofx.*

class MainView : View() {
    override val root = BorderPane()

    private val applicationModel: ApplicationModel = Injector.instance.applicationModel()

    private val treeView: TreeView by inject()
    private val detailView: DetailView by inject()

    init {
        title = "WMS Notes"

        with(root) {
            setPrefSize(940.0, 610.0)
            top<ToolbarView>()
            center = splitpane {
                orientation = Orientation.HORIZONTAL
                this += treeView
                this += detailView
            }
            bottom<StatusBarView>()
        }

        applicationModel
                .selectedNoteUpdates
                .observeOn(JavaFxScheduler.platform())
                .subscribe {
                    title = "WMS Notes - ${it.title}"
                }
    }
}