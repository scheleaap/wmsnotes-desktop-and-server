package info.maaskant.wmsnotes.desktop.design

import javafx.scene.paint.Color
import tornadofx.*

class Styles : Stylesheet() {
    companion object {
        val borderlessButton by cssclass()
    }

    init {
        borderlessButton {
            backgroundColor += Color.TRANSPARENT
            padding = box(0.1.em)
            minHeight = 1.em

            and(hover) {
            }
        }
    }
}