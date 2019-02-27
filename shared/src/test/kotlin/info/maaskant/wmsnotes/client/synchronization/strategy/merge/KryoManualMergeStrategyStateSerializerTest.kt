package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest
import org.junit.jupiter.api.Disabled

@Disabled("Tests written while traveling, code to be implemented next")
internal class KryoManualMergeStrategyStateSerializerTest : KryoSerializerTest<ManualMergeStrategyState>() {
    private val event1 = modelEvent(eventId = 1, noteId = 1, revision = 1)
    private val event2 = modelEvent(eventId = 2, noteId = 1, revision = 2)

    override val items: List<ManualMergeStrategyState> = listOf(
            ManualMergeStrategyState(conflicts = emptyMap(), solutions = emptyMap()),
            ManualMergeStrategyState(conflicts = mapOf(noteId to ManualMergeStrategy.ConflictData(baseNote = Note(), localNote = Note(), remoteNote = Note())), solutions = emptyMap()),
            ManualMergeStrategyState(conflicts = emptyMap(), solutions = mapOf(noteId to MergeStrategy.MergeResult.Solution(newLocalEvents = listOf(event1), newRemoteEvents = listOf(event2))))
    )

    override fun createInstance(kryoPool: Pool<Kryo>) = KryoManualMergeStrategyStateSerializer(kryoPool)

    companion object {
        private const val noteId = "note"
        private const val title = "title"
        private const val content = "text"

        internal fun modelEvent(eventId: Int, noteId: Int, revision: Int): Event {
            return NoteCreatedEvent(eventId = eventId, noteId = "note-$noteId", revision = revision, path = TODO(), title = "Title $noteId", content = TODO())
        }
    }
}
