package info.maaskant.wmsnotes.model.synchronization;

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import java.io.File

interface StateProperty {
    fun put(value: Int)
    fun get(): Int?
}

class InMemoryStateProperty() : StateProperty {
    private var value: Int? = null

    override fun put(value: Int) {
        this.value = value
    }

    override fun get(): Int? = value
}

//class KryoFileStateProperty(private val file: File, private val kryo: Kryo) : StateProperty {
//    override fun put(value: Int)  {
//        file.outputStream().use { outputStream ->
//            Output(outputStream).use { output ->
//                output.writeInt(value)
//            }
//        }
//    }
//
//    override fun get(): Int {
//        file.inputStream().use { inputStream ->
//            return Input(inputStream).use { input ->
//                input.readInt()
//            }
//        }
//    }
//}

class SimpleFileStateProperty(private val file: File) : StateProperty {
    override fun put(value: Int) {
        file.writeBytes(value.toString().toByteArray())
    }

    override fun get(): Int? {
        return file.readBytes().toString().toInt()
    }
}

class CachingStateProperty(private val wrapped: StateProperty) : StateProperty {
    private var cached: Boolean = false
    private var value: Int? = null

    override fun put(value: Int) {
        this.cached = false
        wrapped.put(value)
        this.value = value
        this.cached = true
    }

    override fun get(): Int? {
        return if (cached) value else wrapped.get()
    }
}
