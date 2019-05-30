package info.maaskant.wmsnotes.debug

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.Event
import info.maaskant.wmsnotes.model.KryoEventSerializer
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateCache
import info.maaskant.wmsnotes.model.aggregaterepository.AggregateRepository
import info.maaskant.wmsnotes.model.aggregaterepository.CachingAggregateRepository
import info.maaskant.wmsnotes.model.aggregaterepository.NoopAggregateCache
import info.maaskant.wmsnotes.model.eventstore.EventStore
import info.maaskant.wmsnotes.model.eventstore.FileEventStore
import info.maaskant.wmsnotes.model.note.Note
import info.maaskant.wmsnotes.utilities.serialization.Serializer
import java.io.File

fun main(args: Array<String>) {
    dump(File("D:\\Progs\\Java\\wmsnotes-desktop\\tmp\\desktop"))
    dump(File("D:\\Progs\\Java\\wmsnotes-desktop\\tmp\\android"))
}

private fun dump(rootDirectory: File) {
    val outputDirectory = rootDirectory.resolve("dump").also {
        it.mkdirs()
    }
    val eventStore = eventStore(rootDirectory, eventSerializer(kryoPool()))
    val noteRepository = noteRepository(eventStore, noteCache())
    eventStore.getAggregateIds()
            .filter { it.startsWith("n-") }
            .map { noteRepository.getLatest(aggId = it) }
            .subscribe {
                println(it.aggId)
                val file = outputDirectory.resolve(it.aggId + ".md")
                writeNoteToFile(it, file)
            }
}

fun writeNoteToFile(note: Note, file: File) {
    if (note.attachments.isNotEmpty()) throw NotImplementedError()
    val content = """---
aggId: ${note.aggId}
exists: ${note.exists}
path: ${note.path}
title: ${note.title}
---
${note.content}"""
    file.writeText(content)
}

fun eventSerializer(kryoPool: Pool<Kryo>): Serializer<Event> = KryoEventSerializer(kryoPool)

fun eventStore(appDirectory: File, eventSerializer: Serializer<Event>): FileEventStore =
        FileEventStore(appDirectory.resolve("events"), eventSerializer)

fun kryoPool(): Pool<Kryo> {
    return object : Pool<Kryo>(true, true) {
        override fun create(): Kryo = Kryo()
    }
}

fun noteCache(): AggregateCache<Note> = NoopAggregateCache()

fun noteRepository(eventStore: EventStore, cache: AggregateCache<Note>): AggregateRepository<Note> =
        CachingAggregateRepository(eventStore, cache, Note())
