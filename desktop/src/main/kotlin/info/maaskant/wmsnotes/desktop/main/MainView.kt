package info.maaskant.wmsnotes.desktop.main

import com.github.thomasnield.rxkotlinfx.observeOnFx
import info.maaskant.wmsnotes.desktop.main.editing.EditingView
import info.maaskant.wmsnotes.desktop.main.editing.EditingViewModel
import io.reactivex.rxkotlin.Observables
import javafx.geometry.Orientation
import javafx.scene.layout.BorderPane
import tornadofx.*

class MainView : View() {
    private val applicationTitle = "WMS Notes"

    override val root = BorderPane()

    private val navigationViewModel: NavigationViewModel by di()
    private val editingViewModel: EditingViewModel by di()

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

        Observables.combineLatest(
                navigationViewModel.currentSelection,
                editingViewModel.isDirty()
        )
                .observeOnFx()
                .subscribe { (selection, isDirty) ->
                    val nodeTitle = when (selection) {
                        NavigationViewModel.Selection.Nothing -> ""
                        is NavigationViewModel.Selection.NoteSelection -> " - ${selection.title}"
                        is NavigationViewModel.Selection.FolderSelection -> " - ${selection.title}"
                    }
                    val dirtyMarker = if (isDirty) "*" else ""
                    title = "$applicationTitle$nodeTitle$dirtyMarker"
                }
    }
}