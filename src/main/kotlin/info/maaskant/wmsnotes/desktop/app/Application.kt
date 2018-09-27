package info.maaskant.wmsnotes.desktop.app

import javafx.scene.image.Image
import tornadofx.App
import tornadofx.addStageIcon
import info.maaskant.wmsnotes.desktop.view.MainView

class Application : App(MainView::class, Styles::class) {
    init {
        addStageIcon(Image("app-icon.png"))
    }
}
