package info.maaskant.wmsnotes.desktop.client.indexing

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import com.google.common.collect.ImmutableListMultimap
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest

internal class KryoTreeIndexStateSerializerTest : KryoSerializerTest<TreeIndexState>() {
    private val aggId1 = "note-1"
    private val aggId2 = "note-2"
    private val path1 = Path("path1")
    private val path2 = Path("path2")

    override val items: List<TreeIndexState> = listOf(
            TreeIndexState(isInitialized = false),
            TreeIndexState(isInitialized = true),
            TreeIndexState(isInitialized = true, foldersWithChildren = ImmutableListMultimap.builder<Path, String>()
                    .put(path1, aggId1)
                    .put(path2, aggId2)
                    .build()
            ),
            TreeIndexState(isInitialized = true, notes = mapOf(
                    (aggId1 to Note(aggId = aggId1, path = path1, title = "Title 1")),
                    (aggId2 to Note(aggId = aggId2, path = path2, title = "Title 2"))
            )),
            TreeIndexState(isInitialized = true, autoFolders = setOf(aggId1, aggId2))

    )

    override fun createInstance(kryoPool: Pool<Kryo>) = KryoTreeIndexStateSerializer(kryoPool)
}
