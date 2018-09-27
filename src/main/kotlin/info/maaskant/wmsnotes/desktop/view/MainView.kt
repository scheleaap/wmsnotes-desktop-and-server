package info.maaskant.wmsnotes.desktop.view

import com.github.thomasnield.rxkotlinfx.actionEvents
import javafx.application.Platform
import javafx.geometry.Orientation
import javafx.scene.Group
import javafx.scene.control.TreeItem
import javafx.scene.layout.BorderPane
import tornadofx.*

class MainView : View() {
    override val root = BorderPane()

    private val testView: TestView by inject()
    private val nodeView: NodeView by inject()

    init {
        title = "WMS Notes"

        with(root) {
            setPrefSize(940.0, 610.0)
            center = splitpane {
                orientation = Orientation.VERTICAL
                this += testView
                this += nodeView
            }
        }
    }
}