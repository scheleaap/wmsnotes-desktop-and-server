package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.projection.Note
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest
import org.junit.jupiter.api.Disabled

@Disabled("Tests written while traveling, code to be implemented next")
internal class KryoManualMergeStrategyStateSerializerTest : KryoSerializerTest<ManualMergeStrategyState>() {
    private val noteId = "note"
    private val event1 = NoteCreatedEvent(eventId = 1, noteId = noteId, revision = 1, title = "Title")
    private val event2 = NoteCreatedEvent(eventId = 2, noteId = noteId, revision = 2, title = "Title")

    override val items: List<ManualMergeStrategyState> = listOf(
            ManualMergeStrategyState(conflicts = emptyMap(), solutions = emptyMap()),
            ManualMergeStrategyState(conflicts = mapOf(noteId to ManualMergeStrategy.ConflictData(baseNote = Note(), localNote = Note(), remoteNote = Note())), solutions = emptyMap()),
            ManualMergeStrategyState(conflicts = emptyMap(), solutions = mapOf(noteId to MergeStrategy.MergeResult.Solution(newLocalEvents = listOf(event1), newRemoteEvents = listOf(event2))))
    )

    override fun createInstance(kryoPool: Pool<Kryo>) = KryoManualMergeStrategyStateSerializer(kryoPool)
}
