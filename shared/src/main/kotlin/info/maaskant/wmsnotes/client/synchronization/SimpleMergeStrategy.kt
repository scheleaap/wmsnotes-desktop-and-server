package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.client.synchronization.MergeStrategy.MergeResult
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.projection.Note
import io.reactivex.Observable
import javax.inject.Inject

class SimpleMergeStrategy @Inject constructor(
        differenceAnalyzer: DifferenceAnalyzer,
        differenceCompensator: DifferenceCompensator
) : MergeStrategy {
    override fun merge(
            localEvents: List<Event>,
            remoteEvents: List<Event>,
            baseNote: Note,
            localNote: Note,
            remoteNote: Note
    ): MergeResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}