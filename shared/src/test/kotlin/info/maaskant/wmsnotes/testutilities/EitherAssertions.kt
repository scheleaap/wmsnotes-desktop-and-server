package info.maaskant.wmsnotes.testutilities

import arrow.core.Either
import assertk.Assert
import assertk.assertions.isInstanceOf

object EitherAssertions {
    fun <A, B> Assert<Either<A, B>>.isLeft() = transform { actual ->
        assertThat(actual).isInstanceOf(Either.Left::class)
    }

    fun <A, B> Assert<Either<A, B>>.isRight() = transform { actual ->
        assertThat(actual).isInstanceOf(Either.Right::class)
    }
}