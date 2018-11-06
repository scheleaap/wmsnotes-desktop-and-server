package info.maaskant.wmsnotes.desktop.main

import com.github.thomasnield.rxkotlinfx.observeOnFx
import info.maaskant.wmsnotes.desktop.main.editing.EditingView
import javafx.geometry.Orientation
import javafx.scene.layout.BorderPane
import tornadofx.*

class MainView : View() {
    private val applicationTitle = "WMS Notes"

    override val root = BorderPane()

    private val applicationModel: ApplicationModel by di()

    private val treeView: TreeView by inject()
    private val editingView: EditingView by inject()

    init {
        title = applicationTitle

        with(root) {
            setPrefSize(940.0, 610.0)
            top<MenuAndToolbarView>()
            center = splitpane {
                orientation = Orientation.HORIZONTAL
                setDividerPosition(0, 0.3)
                this += treeView
                this += editingView
            }
            bottom<StatusBarView>()
        }

        applicationModel
                .selectedNote
                .observeOnFx()
                .subscribe {
                    if (it.value != null) {
                        title = "$applicationTitle - ${it.value!!.title}"
                    } else {
                        title = applicationTitle
                    }
                }
    }
}