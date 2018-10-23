package info.maaskant.wmsnotes.model.projection.cache

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import info.maaskant.wmsnotes.model.projection.Note
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class KryoNoteSerializer @Inject constructor() : NoteSerializer, Serializer<Note>() {

    private val kryo = Kryo()

    init {
        kryo.register(Note::class.java, this)
    }

    override fun serialize(note: Note): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        byteArrayOutputStream.use { baos ->
            Output(baos).use { ko -> kryo.writeClassAndObject(ko, note) }
        }
        return byteArrayOutputStream.toByteArray()
    }

    override fun deserialize(bytes: ByteArray): Note {
        return Input(bytes).use { ki ->
            kryo.readClassAndObject(ki) as Note
        }
    }

    override fun write(kryo: Kryo, output: Output, it: Note) {
        output.writeInt(it.revision, true)
        output.writeBoolean(it.exists)
        output.writeString(it.noteId)
        output.writeString(it.title)
        output.writeInt(it.attachments.size)
        for ((name, content) in it.attachments) {
            output.writeString(name)
            output.writeInt(content.size, true)
            output.writeBytes(content)
            output.writeString(it.attachmentHashes[name])
        }
    }

    override fun read(kryo: Kryo, input: Input, clazz: Class<out Note>): Note {
        val revision = input.readInt(true)
        val exists = input.readBoolean()
        val noteId = input.readString()
        val title = input.readString()
        val numberOfAttachments = input.readInt()
        val attachments = HashMap<String, ByteArray>(numberOfAttachments)
        val attachmentHashes = HashMap<String, String>(numberOfAttachments)
        for (i in 1..numberOfAttachments) {
            val name = input.readString()
            val length = input.readInt(true)
            val content = input.readBytes(length)
            val hash = input.readString()
            attachments[name] = content
            attachmentHashes[name] = hash
        }
        return Note.deserialize(
                revision = revision,
                exists = exists,
                noteId = noteId,
                title = title,
                attachments = attachments,
                attachmentHashes = attachmentHashes
        )
    }

}
