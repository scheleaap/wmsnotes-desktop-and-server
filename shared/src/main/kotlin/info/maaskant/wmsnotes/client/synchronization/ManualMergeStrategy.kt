package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.synchronization.MergeStrategy.MergeResult
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.projection.Note
import io.reactivex.Observable

class ManualMergeStrategy : MergeStrategy {
    override fun merge(
            localEvents: List<Event>,
            remoteEvents: List<Event>,
            baseNote: Note,
            localNote: Note,
            remoteNote: Note
    ): MergeResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getConflictData(noteId: String): ConflictData {
        TODO()
    }

    fun getConflictedNoteIds(): Observable<Set<String>> {
        TODO()
    }

    fun resolve(noteId: String, choice: ConflictResolutionChoice) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    data class ConflictData(
//            val noteId: String,
//            val localEvents: List<Event>,
//            val remoteEvents: List<Event>,
            val baseNote: Note,
            val localNote: Note,
            val remoteNote: Note
    )

    enum class ConflictResolutionChoice {
        LOCAL,
        REMOTE,
//        BOTH,
    }
}