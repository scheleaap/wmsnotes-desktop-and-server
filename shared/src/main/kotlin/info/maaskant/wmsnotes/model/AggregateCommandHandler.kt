package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.utilities.Optional
import javax.inject.Inject

interface CommandHandler {
    fun handle(command: Command): Optional<Event>
}

class AggregateCommandHandler<T : Aggregate<T>> @Inject constructor(
        private val repository: AggregateRepository<T>,
        private val commandToEventMapper: CommandToEventMapper
) : CommandHandler {
    override fun handle(command: Command): Optional<Event> {
        val eventIn: Event = commandToEventMapper.map(command)
        val aggregate: T = repository.get(eventIn.aggId, eventIn.revision - 1)
        val (_, eventOut) = aggregate.apply(eventIn)
        return Optional(eventOut)
    }
}