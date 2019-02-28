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
    fun `create with varargs, illegal character`() {
        assertThrows<IllegalArgumentException> {
            Path("el/1", "el2", "el3")
        }
        assertThrows<IllegalArgumentException> {
            Path("el1", "el/2", "el3")
        }
        assertThrows<IllegalArgumentException> {
            Path("el1", "el2", "el/3")
        }
    }

    @Test
    fun `create with varargs, empty element`() {
        assertThrows<IllegalArgumentException> {
            Path("el1", "", "el3")
        }
    }

    @Test
    fun `create with varargs, blank element`() {
        assertThrows<IllegalArgumentException> {
            Path("el1", " ", "el3")
        }
    }

    @Test
    fun `create with varargs, no elements`() {
        assertThrows<IllegalArgumentException> {
            Path()
        }
    }

    @Test
    fun `create from string`() {
        // When
        val path = Path.from("el1/el2/el3")

        // Then
        assertThat(path.elements).isEqualTo(listOf("el1", "el2", "el3"))
    }

    @Test
    fun `create from string, blank`() {
        assertThrows<IllegalArgumentException> {
            Path.from(" ")
        }
    }

    @Test
    fun `create from string, slash problem 1`() {
        assertThrows<IllegalArgumentException> {
            Path.from("/")
        }
    }

    @Test
    fun `create from string, slash problem 2`() {
        assertThrows<IllegalArgumentException> {
            Path.from("a//b")
        }
    }


}