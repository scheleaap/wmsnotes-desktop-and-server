package info.maaskant.wmsnotes.client.synchronization.strategy.merge

//import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult
//import info.maaskant.wmsnotes.model.Event
//import info.maaskant.wmsnotes.model.note.Note
//import io.reactivex.Observable
//import javax.inject.Inject
//
//@Suppress("UNUSED_PARAMETER")
//class ManualMergeStrategy @Inject constructor(
//        differenceAnalyzer: DifferenceAnalyzer,
//        differenceCompensator: DifferenceCompensator
//) : MergeStrategy {
//    override fun merge(
//            localEvents: List<Event>,
//            remoteEvents: List<Event>,
//            baseNote: Note,
//            localNote: Note,
//            remoteNote: Note
//    ): MergeResult {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    fun getConflictData(aggId: String): ConflictData {
//        TODO()
//    }
//
//    fun getConflictedAggregateIds(): Observable<Set<String>> {
//        TODO()
//    }
//
//    fun resolve(aggId: String, choice: ConflictResolutionChoice) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    data class ConflictData(
////            val aggId: String,
////            val localEvents: List<Event>,
////            val remoteEvents: List<Event>,
//            val baseNote: Note,
//            val localNote: Note,
//            val remoteNote: Note
//    )
//
//    enum class ConflictResolutionChoice {
//        LOCAL,
//        REMOTE,
//        BOTH,
//    }
//}