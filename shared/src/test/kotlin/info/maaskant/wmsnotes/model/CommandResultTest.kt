package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.CommandResult.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CommandResultTest {
    @Test
    fun test() {
        assertThat(Failure(1)).isNotEqualTo(Success(1))
    }

    @Test
    fun failure() {
        assertThat(Failure(1)).isEqualTo(Failure(1))
        assertThat(Failure(1)).isNotEqualTo(Failure(2))
    }

    @Test
    fun success() {
        assertThat(Success(1)).isEqualTo(Success(1))
        assertThat(Success(1)).isNotEqualTo(Success(2))
    }
}