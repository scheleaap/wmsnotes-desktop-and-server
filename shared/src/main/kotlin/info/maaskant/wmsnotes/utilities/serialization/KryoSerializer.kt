package info.maaskant.wmsnotes.utilities.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.Pool
import com.google.common.collect.ImmutableListMultimap
import java.io.ByteArrayOutputStream
import javax.inject.Inject

abstract class KryoSerializer<T : Any> @Inject constructor(
        private val kryoPool: Pool<Kryo>,
        private vararg val registrations: Registration
) : info.maaskant.wmsnotes.utilities.serialization.Serializer<T> {

    override fun serialize(o: T): ByteArray {
        val kryo = obtainInitializedKryoInstance()
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            byteArrayOutputStream.use { baos ->
                Output(baos).use { ko -> kryo.writeClassAndObject(ko, o) }
            }
            return byteArrayOutputStream.toByteArray()
        } finally {
            kryoPool.free(kryo)
        }
    }

    override fun deserialize(bytes: ByteArray): T {
        val kryo = obtainInitializedKryoInstance()
        try {
            return Input(bytes).use { ki ->
                @Suppress("UNCHECKED_CAST")
                kryo.readClassAndObject(ki) as T
            }
        } finally {
            kryoPool.free(kryo)
        }
    }

    private fun obtainInitializedKryoInstance(): Kryo {
        val kryo = kryoPool.obtain()
        for (registration in registrations) {
            kryo.register(registration)
        }
        return kryo
    }
}

fun Input.readNullableInt(optimizePositive: Boolean = true): Int? {
    val isNotNull = readBoolean()
    return if (isNotNull) {
        readInt(optimizePositive)
    } else {
        null
    }
}

fun Output.writeNullableInt(value: Int?, optimizePositive: Boolean = true) {
    writeBoolean(value != null)
    if (value != null) {
        writeInt(value, optimizePositive)
    }
}

fun <T, U> Input.readMap(function: () -> Pair<T, U>): Map<T, U> {
    val numberOfItems = readInt()
    val items: MutableMap<T, U> = HashMap()
    for (i in 1..numberOfItems) {
        val (key, value) = function()
        items[key] = value
    }
    return items
}

fun <T, U> Input.readMapWithNullableValues(function: () -> Pair<T, U?>): Map<T, U?> {
    val numberOfItems = readInt()
    val items: MutableMap<T, U?> = HashMap()
    for (i in 1..numberOfItems) {
        val (key, value) = function()
        items[key] = value
    }
    return items
}

fun <T, U> Output.writeMap(items: Map<T, U>, function: (key: T, value: U) -> Unit) {
    writeInt(items.size)
    for ((key, value) in items) {
        function(key, value)
    }
}

fun <T, U> Output.writeMapWithNullableValues(items: Map<T, U?>, function: (key: T, value: U?) -> Unit) {
    writeInt(items.size)
    for ((key, value) in items) {
        function(key, value)
    }
}

fun <T, U> Input.readImmutableListMultimap(function: () -> Pair<T, U>): ImmutableListMultimap<T, U> {
    val numberOfItems = readInt()
    val builder: ImmutableListMultimap.Builder<T, U> = ImmutableListMultimap.builder<T, U>()
    for (i in 1..numberOfItems) {
        val (key, value) = function()
        builder.put(key, value)
    }
    return builder.build()
}

fun <T, U> Output.writeImmutableListMultimap(items: ImmutableListMultimap<T, U>, function: (key: T, value: U) -> Unit) {
    writeInt(items.size())
    for ((key, value) in items.entries()) {
        function(key, value)
    }
}

fun <T> Input.readSet(function: () -> T): Set<T> {
    val numberOfItems = readInt()
    val items: MutableSet<T> = HashSet()
    for (i in 1..numberOfItems) {
        val value = function()
        items += value
    }
    return items
}

fun <T> Output.writeSet(values: Set<T>, function: (value: T) -> Unit) {
    writeInt(values.size)
    for (value in values) {
        function(value)
    }
}
