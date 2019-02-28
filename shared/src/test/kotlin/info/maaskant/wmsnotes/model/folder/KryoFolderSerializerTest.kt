//package info.maaskant.wmsnotes.model.folder
//
//import com.esotericsoftware.kryo.Kryo
//import com.esotericsoftware.kryo.util.Pool
//import info.maaskant.wmsnotes.model.Path
//import info.maaskant.wmsnotes.model.folder.Folder
//import info.maaskant.wmsnotes.utilities.serialization.KryoSerializerTest
//
//internal class KryoFolderSerializerTest : KryoSerializerTest<Folder>() {
//    private val folderId = "folder"
//
//    override val items: List<Folder> = listOf(Folder()
//            .apply(TODO()).component1()
//    )
//
//    override fun createInstance(kryoPool: Pool<Kryo>) = KryoFolderSerializer(kryoPool)
//}