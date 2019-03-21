package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.note.NoteCreatedEvent
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest
import org.junit.jupiter.api.Disabled

@Disabled("Tests written while traveling, code to be implemented next")
internal class KryoManualMergeStrategyStateSerializerTest : KryoSerializerTest<ManualMergeStrategyState>() {
    private val event1 = modelEvent(eventId = 1, aggId = 1, revision = 1)
    private val event2 = modelEvent(eventId = 2, aggId = 1, revision = 2)

    override val items: List<ManualMergeStrategyState> = listOf(
            ManualMergeStrategyState(conflicts = emptyMap(), solutions = emptyMap()),
            ManualMergeStrategyState(conflicts = mapOf(aggId to ManualMergeStrategy.ConflictData(baseNote = Note(), localNote = Note(), remoteNote = Note())), solutions = emptyMap()),
            ManualMergeStrategyState(conflicts = emptyMap(), solutions = mapOf(aggId to MergeStrategy.MergeResult.Solution(newLocalEvents = listOf(event1), newRemoteEvents = listOf(event2))))
    )

    override fun createInstance(kryoPool: Pool<Kryo>) = KryoManualMergeStrategyStateSerializer(kryoPool)

    companion object {
        private const val aggId = "note"
        private const val title = "title"
        private const val content = "text"

        internal fun modelEvent(eventId: Int, aggId: Int, revision: Int): Event {
            return NoteCreatedEvent(eventId = eventId, aggId = "note-$aggId", revision = revision, path = Path("path-$aggId"), title = "Title $aggId", content = "Text $aggId")
        }
    }
}
