package info.maaskant.wmsnotes.model

import arrow.core.None
import arrow.core.Option

sealed class CommandError {
    data class IllegalStateError(val message: String) : CommandError()
    data class InvalidCommandError(val message: String) : CommandError()
    data class NetworkError(val message: String, val cause: Option<Throwable> = None) : CommandError()
    data class OtherError(val message: String, val cause: Option<Throwable> = None) : CommandError()
    data class StorageError(val message: String, val cause: Option<Throwable> = None) : CommandError()
}
