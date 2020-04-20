package info.maaskant.wmsnotes.client.synchronization.strategy.merge.folder

import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergingSynchronizationStrategy
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.folder.Folder
import info.maaskant.wmsnotes.model.folder.FolderEvent

class FolderMergingSynchronizationStrategy(
        mergeStrategy: MergeStrategy<Folder>,
        aggregateRepository: AggregateRepository<Folder>
) : MergingSynchronizationStrategy<Folder>(mergeStrategy, aggregateRepository) {
    override fun canHandleEvent(it: Event): Boolean = it is FolderEvent
}