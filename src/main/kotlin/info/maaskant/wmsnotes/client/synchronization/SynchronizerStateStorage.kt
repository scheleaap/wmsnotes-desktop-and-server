package info.maaskant.wmsnotes.client.synchronization;

import org.mapdb.DB
import org.mapdb.Serializer

interface SynchronizerStateStorage {
    val lastLocalRevisions: MutableMap<String, Int?>
    val lastRemoteRevisions: MutableMap<String, Int?>
}

class InMemorySynchronizerStateStorage() : SynchronizerStateStorage {
    override val lastLocalRevisions = HashMap<String, Int?>().withDefault { null }
    override val lastRemoteRevisions = HashMap<String, Int?>().withDefault { null }
}

class MapDbSynchronizerStateStorage(database: DB) : SynchronizerStateStorage {
    override val lastLocalRevisions = database
            .hashMap("lastLocalRevisions", Serializer.STRING, Serializer.INTEGER)
            .valueLoader { null }
            .createOrOpen()
    override val lastRemoteRevisions = database
            .hashMap("lastRemoteRevisions", Serializer.STRING, Serializer.INTEGER)
            .valueLoader { null }
            .createOrOpen()
}
