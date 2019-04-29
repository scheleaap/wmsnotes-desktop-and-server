package info.maaskant.wmsnotes.model.note.policy

import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.CommandBus
import info.maaskant.wmsnotes.model.CommandRequest
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.note.ChangeTitleCommand
import info.maaskant.wmsnotes.model.note.ContentChangedEvent
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

@Suppress("SameParameterValue")
internal class NoteTitlePolicyTest {
    private val aggId = "note"
    val revision = 11

    private lateinit var commandBus: CommandBus
    private val eventStore: EventStore = mockk()
    private lateinit var eventSubject: PublishSubject<Event>
    private val scheduler = Schedulers.trampoline()

    @BeforeEach
    fun init() {
        clearMocks(
                eventStore
        )
        commandBus = CommandBus()
        eventSubject = PublishSubject.create()
        every { eventStore.getEventUpdates() }.returns(eventSubject)
    }

    @Test
    fun `content changed event`() {
        // Given
        val content = UUID.randomUUID().toString()
        val title = UUID.randomUUID().toString()
        val requestObserver = commandBus.requests.test()
        val titleExtractor: (String) -> String = { if (it == content) title else "" }
        val event = ContentChangedEvent(eventId = 0, aggId = aggId, revision = revision, content = content)
        createInstance(titleExtractor)

        // When
        eventSubject.onNext(event)

        // Then
        assertThatOneChangeTitleCommandWasRequested(requestObserver, aggId, title, revision)
    }

    @Test
    fun `other event`() {
        // Given
        val requestObserver = commandBus.requests.test()
        val titleExtractor: (String) -> String = { "foo" }
        val event: Event = mockk()
        createInstance(titleExtractor)

        // When
        eventSubject.onNext(event)

        // Then
        assertThatNoCommandWasRequested(requestObserver)
    }

    private fun assertThatNoCommandWasRequested(requestObserver: TestObserver<CommandRequest<Command>>) {
        requestObserver.assertNoErrors()
        requestObserver.assertNoValues()
    }

    private fun assertThatOneChangeTitleCommandWasRequested(requestObserver: TestObserver<CommandRequest<Command>>, aggId: String, title: String, revision: Int) {
        requestObserver.assertNoErrors()
        requestObserver.assertValueCount(1)
        val request = requestObserver.values()[0]
        assertThat(request.aggId).isEqualTo(aggId)
        assertThat(request.commands).isEqualTo(listOf(ChangeTitleCommand(
                aggId = aggId,
                title = title
        )))
        assertThat(request.lastRevision).isEqualTo(revision)
    }

    private fun createInstance(titleExtractor: (String) -> String) {
        NoteTitlePolicy(
                commandBus = commandBus,
                eventStore = eventStore,
                scheduler = scheduler,
                titleExtractor = titleExtractor
        )
    }
}