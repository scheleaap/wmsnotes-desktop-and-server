package info.maaskant.wmsnotes.client.synchronization.strategy.merge.folder

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergingSynchronizationStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergingSynchronizationStrategyTest
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.folder.Folder
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderEvent
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import info.maaskant.wmsnotes.model.note.NoteEvent
import io.mockk.mockk

internal class FolderMergingSynchronizationStrategyTest : MergingSynchronizationStrategyTest<Folder, FolderEvent>() {
    override fun createInstance(mergeStrategy: MergeStrategy<Folder>, aggregateRepository: AggregateRepository<Folder>): MergingSynchronizationStrategy<Folder> =
            FolderMergingSynchronizationStrategy(mergeStrategy, aggregateRepository)

    override fun createEvent(eventId: Int, aggId: Int, revision: Int): FolderEvent =
            FolderCreatedEvent(eventId = eventId, revision = revision, path = Path("path-$aggId"))

    override fun createMockedAggregate(): Folder =
            mockk()

    override fun createOtherEvent(eventId: Int, aggId: Int, revision: Int): NoteEvent =
            NoteCreatedEvent(eventId = eventId, aggId = "note-$aggId", revision = revision, path = Path("path-$aggId"), title = "Title $aggId", content = "Text $aggId")

    override fun createMockedEvent(): FolderEvent =
            mockk()

}