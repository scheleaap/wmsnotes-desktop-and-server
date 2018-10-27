package info.maaskant.wmsnotes.utilities.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal abstract class KryoSerializerTest<T : Any> {

    protected abstract val items: List<T>

    @TestFactory
    fun `serialization and deserialization`(): List<DynamicTest> {
        return items.map { itemBefore ->
            DynamicTest.dynamicTest(itemBefore::class.simpleName) {
                // Given
                val kryoPool: Pool<Kryo> = mockk()
                val kryo = Kryo()
                every { kryoPool.obtain() }.returns(kryo)
                every { kryoPool.free(kryo) }.just(Runs)

                val serializer = createInstance(kryoPool)

                // When
                val eventAfter = serializer.deserialize(serializer.serialize(itemBefore))

                // Then
                assertThat(eventAfter).isEqualTo(itemBefore)
                assertThat(eventAfter.hashCode()).isEqualTo(itemBefore.hashCode())
            }
        }
    }

    protected abstract fun createInstance(kryoPool: Pool<Kryo>): Serializer<T>

}