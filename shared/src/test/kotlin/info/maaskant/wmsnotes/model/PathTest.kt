package info.maaskant.wmsnotes.model

import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PathTest {
    @Test
    fun `create with varargs, normal`() {
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
        // When
        val path = Path()

        // Then
        assertThat(path.elements).isEqualTo(emptyList<String>())
    }

    @Test
    fun `create from string, normal`() {
        // When
        val path = Path.from("el1/el2/el3")

        // Then
        assertThat(path).isEqualTo(Path("el1", "el2", "el3"))
    }

    @Test
    fun `create from string, empty`() {
        // When
        val path = Path.from("")

        // Then
        assertThat(path).isEqualTo(Path())
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

    @Test
    fun `create from parent, normal`() {
        // Given
        val path = Path("el1")

        // When
        val childPath = path.child("el2")

        // Then
        assertThat(childPath).isEqualTo(Path("el1", "el2"))
    }

    @Test
    fun `create from parent, illegal character`() {
        assertThrows<IllegalArgumentException> {
            Path().child("el/1")
        }
    }

    @Test
    fun `create from parent, empty element`() {
        assertThrows<IllegalArgumentException> {
            Path().child("")
        }
    }

    @Test
    fun `create from parent, blank element`() {
        assertThrows<IllegalArgumentException> {
            Path().child(" ")
        }
    }

    @Test
    fun `to string, normal`() {
        // When
        val path = Path("el1", "el2", "el3")

        // Then
        assertThat(path.toString()).isEqualTo("el1/el2/el3")
    }

    @Test
    fun `to string, no elements`() {
        // When
        val path = Path()

        // Then
        assertThat(path.toString()).isEqualTo("")
    }

    @Test
    fun equals() {
        // When
        val path1 = Path("el1", "el2")
        val path2 = Path("el3")

        // Then
        assertThat(path1).isEqualTo(path1)
        assertThat(path1).isNotEqualTo(path2)
    }

    @Test
    fun `parent, 1`() {
        // Given
        val path = Path("el1", "el2")

        // When
        val parentPath = path.parent()

        // Then
        assertThat(parentPath).isEqualTo(Path("el1"))
    }

    @Test
    fun `parent, 2`() {
        // Given
        val path = Path("el1")

        // When
        val parentPath = path.parent()

        // Then
        assertThat(parentPath).isEqualTo(Path())
    }

    @Test
    fun `parent, 3`() {
        // Given
        val path = Path()

        // When / then
        assertThrows<IllegalStateException> {
            path.parent()
        }
    }

    @Test
    fun isRoot() {
        assertThat(Path().isRoot).isEqualTo(true)
        assertThat(Path("a").isRoot).isEqualTo(false)
    }

    @Test
    fun isChildOf() {
        val root = Path()
        val path1 = Path("el1")
        val path2 = Path("el1", "el2")
        val path3 = Path("el1", "el2", "el3")

        assertThat(root.isChildOf(root)).isEqualTo(false)

        assertThat(path1.isChildOf(path1)).isEqualTo(false)
        assertThat(path1.isChildOf(path2)).isEqualTo(false)
        assertThat(path1.isChildOf(path3)).isEqualTo(false)

        assertThat(path2.isChildOf(path1)).isEqualTo(true)
        assertThat(path2.isChildOf(path2)).isEqualTo(false)
        assertThat(path2.isChildOf(path3)).isEqualTo(false)

        assertThat(path3.isChildOf(path1)).isEqualTo(true)
        assertThat(path3.isChildOf(path2)).isEqualTo(true)
        assertThat(path3.isChildOf(path3)).isEqualTo(false)
    }
}