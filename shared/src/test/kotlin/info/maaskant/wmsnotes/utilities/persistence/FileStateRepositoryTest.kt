package info.maaskant.wmsnotes.utilities.persistence

import info.maaskant.wmsnotes.utilities.serialization.Serializer
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.schedulers.Schedulers
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

internal class FileStateRepositoryTest : StateRepositoryTest() {

    private lateinit var tempFile: File
    private val serializer: Serializer<Item> = mockk()

    @BeforeEach
    fun init() {
        val tempDir = createTempDir(this::class.simpleName!!)
        tempFile = tempDir.resolve("dir").resolve("file") // Does not exist yet
        clearMocks(producer, serializer)
    }

    @Test
    fun `connect and save`() {
        // Given
        val content = createRandomContent()
        givenProducedStates(givenASerialization(Item(1), content))
        val repo = createInstance()

        // When
        repo.connect(producer)
        waitAMoment()

        // Then
        assertThat(tempFile.exists())
        assertThat(tempFile.readBytes()).isEqualTo(content)
    }

    override fun createInstance() =
            FileStateRepository<Item>(serializer, tempFile, Schedulers.trampoline(), 0, TimeUnit.SECONDS)

    override fun givenAnItem(item: Item): Item = givenASerialization(item, createRandomContent())

    private fun createRandomContent() = UUID.randomUUID().toString().toByteArray()

    private fun givenASerialization(item: Item, content: ByteArray): Item {
        every { serializer.serialize(item) }.returns(content)
        every { serializer.deserialize(content) }.returns(item)
        return item
    }

}