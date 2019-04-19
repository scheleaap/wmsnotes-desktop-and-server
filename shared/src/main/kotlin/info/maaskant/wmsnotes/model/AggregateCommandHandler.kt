package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.CommandHandler.Result
import info.maaskant.wmsnotes.model.CommandHandler.Result.Handled
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.utilities.Optional
import javax.inject.Inject
import kotlin.reflect.KClass

class AggregateCommandHandler<T : Aggregate<T>> @Inject constructor(
        private val cls: KClass<out AggregateCommand>,
        private val repository: AggregateRepository<T>,
        private val commandToEventMapper: CommandToEventMapper<T>
) : CommandHandler {
    override fun handle(command: Command): Result {
        @Suppress("NO_REFLECTION_IN_CLASS_PATH")
        return if (command is AggregateCommand && cls.isInstance(command)) {
            val aggregate: T = repository.getLatest(command.aggId)
            val eventIn: Event = commandToEventMapper.map(command, lastRevision = aggregate.revision)
            val (_, eventOut) = aggregate.apply(eventIn)
            Handled(Optional(eventOut))
        } else {
            Result.NotHandled
        }
    }
}
