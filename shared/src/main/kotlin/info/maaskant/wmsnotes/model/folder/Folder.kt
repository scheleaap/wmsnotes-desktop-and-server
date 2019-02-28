package info.maaskant.wmsnotes.model.folder

import info.maaskant.wmsnotes.model.Aggregate
import info.maaskant.wmsnotes.model.Event

class Folder private constructor(
        override val revision: Int,
        override val aggId: String
) : Aggregate<Folder> {
    override fun apply(event: Event): Pair<Folder, Event?> {
        return when (event) {
            is FolderEvent -> when (event) {
                is FolderCreatedEvent -> TODO()
                is FolderDeletedEvent -> TODO()
            }
            else -> noChanges()
        }
    }

    private fun noChanges(): Pair<Folder, Event?> {
        return Pair(this, null)
    }
}
