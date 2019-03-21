package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.CommandHandler.Result
import info.maaskant.wmsnotes.model.CommandHandler.Result.Handled
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.utilities.Optional
import javax.inject.Inject
import kotlin.reflect.KClass

interface CommandHandler {
    fun handle(command: Command): Result

    sealed class Result {
        object NotHandled : Result()
        data class Handled(val newEvent: Optional<Event>) : Result()
    }
}

class AggregateCommandHandler<T : Aggregate<T>> @Inject constructor(
        private val cls: KClass<out Command>,
        private val repository: AggregateRepository<T>,
        private val commandToEventMapper: CommandToEventMapper<T>
) : CommandHandler {
    override fun handle(command: Command): Result {
        @Suppress("NO_REFLECTION_IN_CLASS_PATH")
        if (cls.isInstance(command)) {
            val eventIn: Event = commandToEventMapper.map(command)
            val aggregate: T = repository.get(eventIn.aggId, eventIn.revision - 1)
            val (_, eventOut) = aggregate.apply(eventIn)
            return Handled(Optional(eventOut))
        } else {
            return Result.NotHandled
        }
    }
}
