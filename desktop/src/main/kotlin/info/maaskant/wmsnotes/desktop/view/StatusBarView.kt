package info.maaskant.wmsnotes.desktop.view

import com.github.thomasnield.rxkotlinfx.observeOnFx
import info.maaskant.wmsnotes.client.synchronization.Synchronizer
import tornadofx.*
import tornadofx.controlsfx.statusbar

class StatusBarView : View() {

    private val synchronizer: Synchronizer by di()

    override val root = statusbar {}

    init {
        synchronizer.getConflicts()
                .observeOnFx()
                .subscribe {
                    root.text = "${it.size} conflicts"
                }
    }
}
