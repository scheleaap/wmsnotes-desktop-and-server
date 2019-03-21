package info.maaskant.wmsnotes.model.folder

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializer
import info.maaskant.wmsnotes.utilities.serialization.writeMap

class KryoFolderSerializer(kryoPool: Pool<Kryo>) : KryoSerializer<Folder>(
        kryoPool,
        Registration(Folder::class.java, KryoFolderSerializer(), 71)
) {
    private class KryoFolderSerializer : Serializer<Folder>() {

        override fun write(kryo: Kryo, output: Output, it: Folder) {
            output.writeInt(it.revision, true)
            output.writeBoolean(it.exists)
            output.writeString(it.aggId)
            output.writeString(it.path.toString())
            output.writeString(it.title)
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out Folder>): Folder {
            val revision = input.readInt(true)
            val exists = input.readBoolean()
            val aggId = input.readString()
            val path = Path.from(input.readString())
            return Folder.deserialize(
                    revision = revision,
                    exists = exists,
                    aggId = aggId,
                    path = path
            )
        }
    }
}
