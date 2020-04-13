package info.maaskant.wmsnotes.model.note.policy

import arrow.core.Right
import arrow.core.Some
import info.maaskant.wmsnotes.model.*
import info.maaskant.wmsnotes.model.CommandOrigin.LOCAL
import info.maaskant.wmsnotes.model.CommandOrigin.REMOTE
import info.maaskant.wmsnotes.model.note.ChangeTitleCommand
import info.maaskant.wmsnotes.model.note.ContentChangedEvent
import io.mockk.mockk
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.collections.immutable.persistentListOf
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

@Suppress("SameParameterValue")
internal class NoteTitlePolicyTest {
    private val aggId = "note"
    val revision = 11

    private lateinit var commandBus: CommandBus
    private val scheduler = Schedulers.trampoline()

    @BeforeEach
    fun init() {
        commandBus = CommandBus()
    }

    @Test
    fun `content changed event, local origin`() {
        // Given
        val commandRequestsObserver = commandBus.requests.test()
        val content = UUID.randomUUID().toString()
        val title = UUID.randomUUID().toString()
        val event = ContentChangedEvent(eventId = 0, aggId = aggId, revision = revision, content = content)
        val commandResult = CommandResult(0, outcome = persistentListOf(mockk<Command>() to Right(Some(event))), origin = LOCAL)
        val titleExtractor: (String) -> String = { if (it == content) title else "" }
        createInstanceAndStart(titleExtractor)

        // When
        commandBus.results.onNext(commandResult)

        // Then
        assertThatOneChangeTitleCommandWasRequested(commandRequestsObserver, aggId, title, revision, LOCAL)
    }

    @Test
    fun `content changed event, remote origin`() {
        val commandRequestsObserver = commandBus.requests.test()
        val content = UUID.randomUUID().toString()
        val title = UUID.randomUUID().toString()
        val event = ContentChangedEvent(eventId = 0, aggId = aggId, revision = revision, content = content)
        val commandResult = CommandResult(0, outcome = persistentListOf(mockk<Command>() to Right(Some(event))), origin = REMOTE)
        val titleExtractor: (String) -> String = { if (it == content) title else "" }
        createInstanceAndStart(titleExtractor)

        // When
        commandBus.results.onNext(commandResult)

        // Then
        assertThatNoCommandWasRequested(commandRequestsObserver)
    }

    @Test
    fun `other event`() {
        // Given
        val commandRequestsObserver = commandBus.requests.test()
        val event: Event = mockk()
        val commandResult = CommandResult(0, outcome = persistentListOf(mockk<Command>() to Right(Some(event))), origin = LOCAL)
        val titleExtractor: (String) -> String = { "foo" }
        createInstanceAndStart(titleExtractor)

        // When
        commandBus.results.onNext(commandResult)

        // Then
        assertThatNoCommandWasRequested(commandRequestsObserver)
    }

    private fun assertThatNoCommandWasRequested(requestObserver: TestObserver<CommandRequest<Command>>) {
        requestObserver.assertNoErrors()
        requestObserver.assertNoValues()
    }

    private fun assertThatOneChangeTitleCommandWasRequested(requestObserver: TestObserver<CommandRequest<Command>>, aggId: String, title: String, revision: Int, origin: CommandOrigin) {
        requestObserver.assertNoErrors()
        requestObserver.assertValueCount(1)
        val request = requestObserver.values()[0]
        assertThat(request.aggId).isEqualTo(aggId)
        assertThat(request.commands).isEqualTo(listOf(ChangeTitleCommand(
                aggId = aggId,
                title = title
        )))
        assertThat(request.lastRevision).isEqualTo(revision)
        assertThat(request.origin).isEqualTo(origin)
    }

    private fun createInstanceAndStart(titleExtractor: (String) -> String) {
        NoteTitlePolicy(
                commandBus = commandBus,
                scheduler = scheduler,
                titleExtractor = titleExtractor
        )
                .start()
    }
}