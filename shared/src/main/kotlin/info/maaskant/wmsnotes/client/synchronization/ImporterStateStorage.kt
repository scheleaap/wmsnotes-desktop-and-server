package info.maaskant.wmsnotes.client.synchronization;

import org.mapdb.DB

interface ImporterStateStorage {
    var lastEventId: Int?
}

class InMemoryImporterStateStorage() : ImporterStateStorage {
    private var value: Int? = null
    override var lastEventId: Int?
        get() = value
        set(value) {
            this.value = value
        }
}

class MapDbImporterStateStorage(importerType: ImporterType, database: DB) : ImporterStateStorage {
    private val atomicInteger = database.atomicInteger(when (importerType) {
        ImporterType.LOCAL -> "lastImportedLocalEvent"
        ImporterType.REMOTE -> "lastImportedRemoteEvent"
    }).createOrOpen()

    override var lastEventId: Int?
        get() {
            val value = atomicInteger.get()
            return when (value) {
                0 -> null
                else -> value
            }
        }
        set(value) {
            this.atomicInteger.set(when (value) {
                null -> 0
                else -> value
            })
        }

    enum class ImporterType {
        LOCAL,
        REMOTE
    }
}
