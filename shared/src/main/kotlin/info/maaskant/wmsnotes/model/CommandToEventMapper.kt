package info.maaskant.wmsnotes.model

interface CommandToEventMapper<T : Aggregate<T>> {
    fun map(source: Command): Event
}
