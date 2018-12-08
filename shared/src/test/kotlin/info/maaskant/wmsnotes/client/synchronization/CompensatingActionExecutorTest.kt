package info.maaskant.wmsnotes.client.synchronization

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import io.reactivex.Observable
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CompensatingActionExecutorTest {
    // TODO:
    // - remote events, call successful
    // - remote events, call fails
    // - local events, command successful
    // - local events, command fails
    // - local and remote events, successful
    // - local and remote events, remote fails (do not process local)

    @Test
    fun `outbound sync, call successful`() {
        // Given
        val event1 = modelEvent(eventId = 1, noteId = 1, revision = 11)
        val event2 = modelEvent(eventId = 2, noteId = 1, revision = 12)
        val event3 = modelEvent(eventId = 3, noteId = 3, revision = 31)
        every { localEvents.getEvents() }.returns(Observable.just(event1, event2, event3))
        every { remoteEvents.getEvents() }.returns(Observable.empty())
        val remoteRequest1 = givenARemoteResponseForAnEvent(event1, null, remoteSuccess(event1.eventId, event1.revision))
        val remoteRequest2 = givenARemoteResponseForAnEvent(event2, event1.revision, remoteSuccess(event2.eventId, event2.revision))
        val remoteRequest3 = givenARemoteResponseForAnEvent(event3, null, remoteSuccess(event3.eventId, event3.revision))
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteCommandService.postCommand(remoteRequest1)
            localEvents.removeEvent(event1)
            remoteCommandService.postCommand(remoteRequest2)
            localEvents.removeEvent(event2)
            remoteCommandService.postCommand(remoteRequest3)
            localEvents.removeEvent(event3)
        }
        val finalState = stateObserver.values().last()
        Assertions.assertThat(finalState.lastSynchronizedLocalRevisions[event1.noteId]).isEqualTo(event2.revision)
        Assertions.assertThat(finalState.lastSynchronizedLocalRevisions[event3.noteId]).isEqualTo(event3.revision)
    }

    @Test
    fun `outbound sync, no remote events, call fails`() {
        // Given
        val event1 = modelEvent(eventId = -1, noteId = 1, revision = 11)
        val event2 = modelEvent(eventId = -1, noteId = 1, revision = -1)
        val event3 = modelEvent(eventId = 1, noteId = 3, revision = 31)
        every { localEvents.getEvents() }.returns(Observable.just(event1, event2, event3))
        every { remoteEvents.getEvents() }.returns(Observable.empty())
        val remoteRequest1 = givenARemoteResponseForAnEvent(event1, null, remoteError())
        givenARemoteResponseForAnEvent(event2, null, remoteError())
        givenARemoteResponseForAnEvent(event2, event1.revision, remoteError())
        val remoteRequest3 = givenARemoteResponseForAnEvent(event3, null, remoteSuccess(event3.eventId, event3.revision))
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verifySequence {
            localEvents.getEvents()
            remoteCommandService.postCommand(remoteRequest1)
            remoteCommandService.postCommand(remoteRequest3)
            localEvents.removeEvent(event3)
        }
    }

    @Test
    fun `inbound sync, no local events, command successful`() {
        // Given
        val remoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 1)
        val remoteEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 2)
        val remoteEvent3 = modelEvent(eventId = 3, noteId = 3, revision = 1)
        val localEvent1 = modelEvent(eventId = 11, noteId = 1, revision = 11)
        val localEvent2 = modelEvent(eventId = 12, noteId = 1, revision = 12)
        val localEvent3 = modelEvent(eventId = 13, noteId = 3, revision = 31)
        every { localEvents.getEvents() }.returns(Observable.empty())
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteEvent1, remoteEvent2, remoteEvent3))
        val command1 = givenAnEventIsReturnedIfAnotherEventIsProcessedLocally(remoteEvent1, null, localEvent1)
        val command2 = givenAnEventIsReturnedIfAnotherEventIsProcessedLocally(remoteEvent2, localEvent1.eventId, localEvent2)
        val command3 = givenAnEventIsReturnedIfAnotherEventIsProcessedLocally(remoteEvent3, null, localEvent3)
        val s = createSynchronizer()
        val stateObserver = s.getStateUpdates().test()

        // When
        s.synchronize()

        // Then
        verifySequence {
            remoteEvents.getEvents()
            commandProcessor.blockingProcessCommand(command1)
            remoteEvents.removeEvent(remoteEvent1)
            commandProcessor.blockingProcessCommand(command2)
            remoteEvents.removeEvent(remoteEvent2)
            commandProcessor.blockingProcessCommand(command3)
            remoteEvents.removeEvent(remoteEvent3)
        }
        val finalState = stateObserver.values().last()
        Assertions.assertThat(finalState.lastSynchronizedLocalRevisions[remoteEvent1.noteId]).isEqualTo(localEvent2.revision)
        Assertions.assertThat(finalState.lastSynchronizedLocalRevisions[remoteEvent3.noteId]).isEqualTo(localEvent3.revision)
    }

    @Test
    fun `inbound sync, no local events, command fails`() {
        // Given
        val remoteEvent1 = modelEvent(eventId = 1, noteId = 1, revision = 1)
        val remoteEvent2 = modelEvent(eventId = 2, noteId = 1, revision = 2)
        val remoteEvent3 = modelEvent(eventId = 3, noteId = 3, revision = 1)
        val localEvent3 = modelEvent(eventId = 13, noteId = 3, revision = 31)
        every { localEvents.getEvents() }.returns(Observable.empty())
        every { remoteEvents.getEvents() }.returns(Observable.just(remoteEvent1, remoteEvent2, remoteEvent3))
        val command1 = givenProcessingOfARemoteEventFails(remoteEvent1)
        givenProcessingOfARemoteEventFails(remoteEvent2)
        val command3 = givenAnEventIsReturnedIfAnotherEventIsProcessedLocally(remoteEvent3, null, localEvent3)
        val s = createSynchronizer()

        // When
        s.synchronize()

        // Then
        verifySequence {
            commandProcessor.blockingProcessCommand(command1)
            commandProcessor.blockingProcessCommand(command3)
        }
    }

}