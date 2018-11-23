package info.maaskant.wmsnotes.desktop.main.editing

import info.maaskant.wmsnotes.desktop.main.NavigationViewModel
import info.maaskant.wmsnotes.desktop.main.NavigationViewModel.SelectionSwitchingProcessNotification
import info.maaskant.wmsnotes.desktop.main.NavigationViewModel.SelectionSwitchingProcessNotification.*
import info.maaskant.wmsnotes.desktop.main.editing.preview.Renderer
import info.maaskant.wmsnotes.model.ContentChangedEvent
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.utilities.Optional
import io.mockk.*
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class EditingViewModelTest {
    private val note1Id = "note-1"
    private val note2Id = "note-2"
    private val title = "Title"
    private val text = "Text"
    private val note1Notification1 = Note(
            NavigationViewModel.Selection.NoteSelection(note1Id, title),
            info.maaskant.wmsnotes.model.projection.Note()
                    .apply(NoteCreatedEvent(eventId = 1, noteId = note1Id, revision = 1, title = title)).component1()
                    .apply(ContentChangedEvent(eventId = 2, noteId = note1Id, revision = 2, content = text)).component1()
    )
    private val note1Notification2 = Note(
            NavigationViewModel.Selection.NoteSelection(note1Id, title),
            note1Notification1.note
                    .apply(ContentChangedEvent(eventId = 3, noteId = note1Id, revision = 3, content = "Different text")).component1()
    )
    private val note2Notification = Note(
            NavigationViewModel.Selection.NoteSelection(note1Id, title),
            info.maaskant.wmsnotes.model.projection.Note()
                    .apply(NoteCreatedEvent(eventId = 4, noteId = note2Id, revision = 1, title = title)).component1()
                    .apply(ContentChangedEvent(eventId = 5, noteId = note2Id, revision = 2, content = text)).component1()
    )

    private val navigationViewModel: NavigationViewModel = mockk()
    private val renderer: Renderer = mockk()
    private val scheduler = Schedulers.trampoline()
    private lateinit var selectionSwitchingProcess: Subject<SelectionSwitchingProcessNotification>

    @BeforeEach
    fun init() {
        clearMocks(
                navigationViewModel,
                renderer
        )
        selectionSwitchingProcess = PublishSubject.create()
        every { navigationViewModel.setNavigationAllowed(any()) }.just(Runs)
        every { navigationViewModel.selectionSwitchingProcess }.returns(selectionSwitchingProcess)
    }

    @Test
    fun `default values`() {
        // Given
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)
        val dirtyObserver = model.isDirty().test()
        val enabledObserver = model.isEnabled().test()
        val noteObserver = model.getNote().test()
        val textUpdatesForEditorObserver = model.getTextUpdatesForEditor().test()

        // When

        // Then
        assertThat(model.getText()).isEqualTo("")
        assertThat(dirtyObserver.values().toList()).isEqualTo(listOf(false))
        assertThat(enabledObserver.values().toList()).isEqualTo(listOf(false))
        assertThat(noteObserver.values().toList()).isEqualTo(listOf(Optional<info.maaskant.wmsnotes.model.projection.Note>()))
        assertThat(textUpdatesForEditorObserver.values().toList()).isEqualTo(emptyList<String>())
    }

    @Test
    fun `switch from nothing to note`() {
        // Given
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)
        val enabledObserver = model.isEnabled().test()
        val dirtyObserver = model.isDirty().test()
        val noteObserver = model.getNote().test()
        val textUpdatesForEditorObserver = model.getTextUpdatesForEditor().test()

        // When
        selectionSwitchingProcess.onNext(Loading(true))
        selectionSwitchingProcess.onNext(note1Notification1)
        selectionSwitchingProcess.onNext(Loading(false))

        // Then
        assertThat(enabledObserver.values().toList()).isEqualTo(listOf(false, true))
        assertThat(dirtyObserver.values().toList()).isEqualTo(listOf(false))
        assertThat(noteObserver.values().toList()).isEqualTo(listOf(Optional(), Optional(note1Notification1.note)))
        assertThat(model.getText()).isEqualTo(note1Notification1.note.content)
        assertThat(textUpdatesForEditorObserver.values().toList()).isEqualTo(listOf(note1Notification1.note.content))
    }

    @Test
    fun `switch from note to nothing`() {
        // Given
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)
        givenALoadedNote(note1Notification1)
        val enabledObserver = model.isEnabled().test()
        val dirtyObserver = model.isDirty().test()
        val noteObserver = model.getNote().test()
        val textUpdatesForEditorObserver = model.getTextUpdatesForEditor().test()

        // When
        selectionSwitchingProcess.onNext(Nothing)
        selectionSwitchingProcess.onNext(Loading(false))

        // Then
        assertThat(enabledObserver.values().toList()).isEqualTo(listOf(true, false))
        assertThat(dirtyObserver.values().toList()).isEqualTo(listOf(false))
        assertThat(noteObserver.values().toList()).isEqualTo(listOf(Optional(note1Notification1.note), Optional()))
        assertThat(model.getText()).isEqualTo("")
        assertThat(textUpdatesForEditorObserver.values().toList()).isEqualTo(listOf(""))
    }

    @Test
    fun `switch from note to nothing when dirty`() {
        // Given
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)
        givenALoadedNote(note1Notification1)
        model.setText("changed")
        val dirtyObserver = model.isDirty().test()
        val noteObserver = model.getNote().test()
        val textUpdatesForEditorObserver = model.getTextUpdatesForEditor().test()

        // When
        selectionSwitchingProcess.onNext(Nothing)

        // Then
        assertThat(dirtyObserver.values().toList()).isEqualTo(listOf(true))
        assertThat(noteObserver.values().toList()).isEqualTo(listOf(Optional(note1Notification1.note)))
        assertThat(model.getText()).isEqualTo("changed")
        assertThat(textUpdatesForEditorObserver.values().toList()).isEqualTo(emptyList<String>())
    }

    @Test
    fun `switch from note to different note`() {
        // Given
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)
        givenALoadedNote(note1Notification1)
        val enabledObserver = model.isEnabled().test()
        val dirtyObserver = model.isDirty().test()
        val noteObserver = model.getNote().test()
        val textUpdatesForEditorObserver = model.getTextUpdatesForEditor().test()

        // When
        selectionSwitchingProcess.onNext(Loading(true))
        selectionSwitchingProcess.onNext(note2Notification)
        selectionSwitchingProcess.onNext(Loading(false))

        // Then
        assertThat(enabledObserver.values().toList()).isEqualTo(listOf(true, false, true))
        assertThat(dirtyObserver.values().toList()).isEqualTo(listOf(false))
        assertThat(noteObserver.values().toList()).isEqualTo(listOf(Optional(note1Notification1.note), Optional(note2Notification.note)))
        assertThat(model.getText()).isEqualTo(note2Notification.note.content)
        assertThat(textUpdatesForEditorObserver.values().toList()).isEqualTo(listOf(note2Notification.note.content))
    }

    @Test
    fun `switch from note to different note when dirty`() {
        // Given
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)
        givenALoadedNote(note1Notification1)
        model.setText("changed")
        val dirtyObserver = model.isDirty().test()
        val noteObserver = model.getNote().test()
        val textUpdatesForEditorObserver = model.getTextUpdatesForEditor().test()

        // When
        selectionSwitchingProcess.onNext(Loading(true))
        selectionSwitchingProcess.onNext(note2Notification)
        selectionSwitchingProcess.onNext(Loading(false))

        // Then
        assertThat(dirtyObserver.values().toList()).isEqualTo(listOf(true))
        assertThat(noteObserver.values().toList()).isEqualTo(listOf(Optional(note1Notification1.note)))
        assertThat(model.getText()).isEqualTo("changed")
        assertThat(textUpdatesForEditorObserver.values().toList()).isEqualTo(emptyList<String>())
    }

    @Test
    fun `note update`() {
        // Given
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)
        givenALoadedNote(note1Notification1)
        val enabledObserver = model.isEnabled().test()
        val dirtyObserver = model.isDirty().test()
        val noteObserver = model.getNote().test()
        val textUpdatesForEditorObserver = model.getTextUpdatesForEditor().test()

        // When
        selectionSwitchingProcess.onNext(note1Notification2)
        selectionSwitchingProcess.onNext(Loading(false))

        // Then
        assertThat(enabledObserver.values().toList()).isEqualTo(listOf(true))
        assertThat(dirtyObserver.values().toList()).isEqualTo(listOf(false))
        assertThat(noteObserver.values().toList()).isEqualTo(listOf(Optional(note1Notification1.note), Optional(note1Notification2.note)))
        assertThat(model.getText()).isEqualTo(note1Notification2.note.content)
        assertThat(textUpdatesForEditorObserver.values().toList()).isEqualTo(listOf(note1Notification2.note.content))
    }

    @Test
    fun `note update when dirty`() {
        // Given
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)
        givenALoadedNote(note1Notification1)
        model.setText("changed")
        val dirtyObserver = model.isDirty().test()
        val noteObserver = model.getNote().test()
        val textUpdatesForEditorObserver = model.getTextUpdatesForEditor().test()

        // When
        selectionSwitchingProcess.onNext(note1Notification2)
        selectionSwitchingProcess.onNext(Loading(false))

        // Then
        assertThat(dirtyObserver.values().toList()).isEqualTo(listOf(true))
        assertThat(noteObserver.values().toList()).isEqualTo(listOf(Optional(note1Notification1.note), Optional(note1Notification2.note)))
        assertThat(model.getText()).isEqualTo("changed")
        assertThat(textUpdatesForEditorObserver.values().toList()).isEqualTo(emptyList<String>())
    }

    @Test
    fun `note update when dirty, resolving the dirty state`() {
        // Given
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)
        givenALoadedNote(note1Notification1)
        model.setText(note1Notification2.note.content)
        val dirtyObserver = model.isDirty().test()
        val noteObserver = model.getNote().test()
        val textUpdatesForEditorObserver = model.getTextUpdatesForEditor().test()

        // When
        selectionSwitchingProcess.onNext(note1Notification2)
        selectionSwitchingProcess.onNext(Loading(false))

        // Then
        assertThat(dirtyObserver.values().toList()).isEqualTo(listOf(true, false))
        assertThat(noteObserver.values().toList()).isEqualTo(listOf(Optional(note1Notification1.note), Optional(note1Notification2.note)))
        assertThat(model.getText()).isEqualTo(note1Notification2.note.content)
        assertThat(textUpdatesForEditorObserver.values().toList()).isEqualTo(emptyList<String>())
    }

    @Test
    fun `isEnabled, no selection`() {
        // Given
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)
        val enabledObserver = model.isEnabled().test()

        // When
        selectionSwitchingProcess.onNext(Loading(true))
        selectionSwitchingProcess.onNext(Nothing)
        selectionSwitchingProcess.onNext(Loading(false))

        // Then
        assertThat(enabledObserver.values().toList()).isEqualTo(listOf(false))
    }

    @Test
    fun `text and isDirty, normal`() {
        // Given
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)
        givenALoadedNote(note1Notification1)
        val dirtyObserver = model.isDirty().test()

        // When
        model.setText("changed")

        // Then
        assertThat(model.getText()).isEqualTo("changed")
        assertThat(dirtyObserver.values().toList()).isEqualTo(listOf(false, true))
    }

    @Test
    fun `text and isDirty, the same text twice`() {
        // Given
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)
        givenALoadedNote(note1Notification1)
        model.setText("changed")
        val dirtyObserver = model.isDirty().test()

        // When
        model.setText("changed")

        // Then
        assertThat(model.getText()).isEqualTo("changed")
        assertThat(dirtyObserver.values().toList()).isEqualTo(listOf(true))
    }

    @Test
    fun `text and isDirty, resolving the dirty state`() {
        // Given
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)
        givenALoadedNote(note1Notification1)
        model.setText("changed")
        val dirtyObserver = model.isDirty().test()

        // When
        model.setText(note1Notification1.note.content)

        // Then
        assertThat(model.getText()).isEqualTo(note1Notification1.note.content)
        assertThat(dirtyObserver.values().toList()).isEqualTo(listOf(true, false))
    }

    @Test
    fun `text and isDirty, editing disabled`() {
        // Given
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)
        givenALoadedNote(note1Notification1)
        givenEditingIsDisabled()
        val dirtyObserver = model.isDirty().test()

        // When / then
        assertThrows<IllegalStateException> { model.setText("new") }
        assertThat(model.getText()).isEqualTo(note1Notification1.note.content)
        assertThat(dirtyObserver.values().toList()).isEqualTo(listOf(false))
    }

    @Test
    fun `text and isDirty, editing disabled, text equal to note content`() {
        // Given
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)
        givenALoadedNote(note1Notification1)
        givenEditingIsDisabled()
        val dirtyObserver = model.isDirty().test()

        // When
        model.setText(note1Notification1.note.content)

        // Then
        assertThat(model.getText()).isEqualTo(note1Notification1.note.content)
        assertThat(dirtyObserver.values().toList()).isEqualTo(listOf(false))
    }

    @Test
    fun `set navigationAllowed on navigation view model`() {
        // Given

        // When
        val model = EditingViewModel(navigationViewModel, renderer, scheduler = scheduler)

        // Then
        val navigationAllowedSlot = slot<Observable<Boolean>>()
        verify { navigationViewModel.setNavigationAllowed(capture(navigationAllowedSlot)) }

        // Given
        givenALoadedNote(note1Notification1)
        val navigationAllowedObserver = navigationAllowedSlot.captured.test()

        // When
        model.setText("different")

        // Then
        assertThat(navigationAllowedObserver.values().toList()).isEqualTo(listOf(true, false))
    }

    private fun givenEditingIsDisabled() {
        selectionSwitchingProcess.onNext(Loading(true))
    }

    private fun givenALoadedNote(note: Note) {
        selectionSwitchingProcess.onNext(Loading(true))
        selectionSwitchingProcess.onNext(note)
        selectionSwitchingProcess.onNext(Loading(false))
    }

}