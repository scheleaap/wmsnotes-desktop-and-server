package info.maaskant.wmsnotes.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PathTest {
    @Test
    fun `create with varargs`() {
        // When
        val path = Path("el1", "el2", "el3")

        // Then
        assertThat(path.elements).isEqualTo(listOf("el1", "el2", "el3"))
    }

    @Test
    fun `create from string`() {
        // When
        val path = Path.fromString("el1/el2/el3")

        // Then
        assertThat(path.elements).isEqualTo(listOf("el1", "el2", "el3"))
    }

    @Test
    fun `empty path`() {
        assertThrows<IllegalArgumentException> {
            Path()
        }
    }

    @Test
    fun `empty element`() {
        assertThrows<IllegalArgumentException> {
            Path("el1", "", "el3")
        }
    }

    @Test
    fun `blank element`() {
        assertThrows<IllegalArgumentException> {
            Path("el1", " ", "el3")
        }
    }
}