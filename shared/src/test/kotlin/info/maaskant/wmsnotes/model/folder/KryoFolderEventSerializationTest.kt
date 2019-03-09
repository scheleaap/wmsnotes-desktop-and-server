package info.maaskant.wmsnotes.model.folder

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.KryoEventSerializer
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest

internal class KryoFolderEventSerializationTest : KryoSerializerTest<Event>() {
    private val aggId = "folder-1"

    override val items: List<FolderEvent> = listOf(
            FolderCreatedEvent(eventId = 1, revision = 1, path = Path("path1")),
            FolderDeletedEvent(eventId = 1, revision = 1, path = Path("path2"))
            // Add more classes here
    )

    override fun createInstance(kryoPool: Pool<Kryo>) = KryoEventSerializer(kryoPool)
}
