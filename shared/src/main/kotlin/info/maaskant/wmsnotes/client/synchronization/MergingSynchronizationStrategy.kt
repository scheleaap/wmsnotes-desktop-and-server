package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.projection.NoteProjector
import info.maaskant.wmsnotes.utilities.logger

class MergingSynchronizationStrategy(
        private val mergeStrategy: MergeStrategy,
        private val noteProjector: NoteProjector
) : SynchronizationStrategy {
    private val logger by logger()

    override fun resolve(noteId: String, localEvents: List<Event>, remoteEvents: List<Event>): SynchronizationStrategy.ResolutionResult {
        return if (localEvents.isEmpty() || remoteEvents.isEmpty()) {
            logger.warn("Unexpected call to ${this.javaClass.name} with $localEvents and $remoteEvents")
            SynchronizationStrategy.ResolutionResult.NoSolution
        } else {
            val baseNote = noteProjector.project(noteId = noteId, revision = localEvents.first().revision - 1)
            val localNote = NoteProjector.project(baseNote, localEvents)
            val remoteNote = NoteProjector.project(baseNote, remoteEvents)
            val mergeResult = mergeStrategy.merge(
                    localEvents = localEvents,
                    remoteEvents = remoteEvents,
                    baseNote = baseNote,
                    localNote = localNote,
                    remoteNote = remoteNote
            )
            return when (mergeResult) {
                MergeStrategy.MergeResult.NoSolution -> SynchronizationStrategy.ResolutionResult.NoSolution
                is MergeStrategy.MergeResult.Solution -> {
                    SynchronizationStrategy.ResolutionResult.Solution(
                            compensatingAction = CompensatingAction(
                                    compensatedLocalEvents = localEvents,
                                    compensatedRemoteEvents = remoteEvents,
                                    newLocalEvents = mergeResult.newLocalEvents,
                                    newRemoteEvents = mergeResult.newRemoteEvents
                            )
                    )
                }
            }
        }
    }
}