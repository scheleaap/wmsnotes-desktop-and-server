package info.maaskant.wmsnotes.testutilities

import assertk.Assert
import assertk.assertions.support.expected
import java.io.File

object FileAssertions {
    fun Assert<File>.doesNotExist() = given {
        if (it.exists()) {
            expected("expected file $it not to exist, but it does exist")
        }
    }
}