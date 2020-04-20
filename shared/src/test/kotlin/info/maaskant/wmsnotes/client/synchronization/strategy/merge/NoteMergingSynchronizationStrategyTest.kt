package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.note.NoteMergingSynchronizationStrategy
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderEvent
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import info.maaskant.wmsnotes.model.note.NoteEvent
import io.mockk.mockk

internal class NoteMergingSynchronizationStrategyTest : MergingSynchronizationStrategyTest<Note, NoteEvent>() {
    override fun createInstance(mergeStrategy: MergeStrategy<Note>, aggregateRepository: AggregateRepository<Note>): MergingSynchronizationStrategy<Note> =
            NoteMergingSynchronizationStrategy(mergeStrategy, aggregateRepository)

    override fun createEvent(eventId: Int, aggId: Int, revision: Int): NoteEvent =
            NoteCreatedEvent(eventId = eventId, aggId = "note-$aggId", revision = revision, path = Path("path-$aggId"), title = "Title $aggId", content = "Text $aggId")

    override fun createMockedAggregate(): Note =
            mockk()

    override fun createOtherEvent(eventId: Int, aggId: Int, revision: Int): FolderEvent =
            FolderCreatedEvent(eventId = eventId, revision = revision, path = Path("path-$aggId"))

    override fun createMockedEvent(): NoteEvent =
            mockk()

}