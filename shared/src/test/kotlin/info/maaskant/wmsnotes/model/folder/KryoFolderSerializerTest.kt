package info.maaskant.wmsnotes.model.folder

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest

internal class KryoFolderSerializerTest : KryoSerializerTest<Folder>() {
    private val aggId = "folder"

    override val items: List<Folder> = listOf(Folder()
            .apply(FolderCreatedEvent(eventId = 1, revision = 1, path = Path("path"))).component1()
    )

    override fun createInstance(kryoPool: Pool<Kryo>) = KryoFolderSerializer(kryoPool)
}