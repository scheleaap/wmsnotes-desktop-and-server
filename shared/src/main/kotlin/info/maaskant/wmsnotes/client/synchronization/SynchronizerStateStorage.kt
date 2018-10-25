package info.maaskant.wmsnotes.client.synchronization;

import org.mapdb.DB
import org.mapdb.Serializer

interface SynchronizerStateStorage {
    val lastLocalRevisions: MutableMap<String, Int?>
    val lastRemoteRevisions: MutableMap<String, Int?>
    val localEventIdsToIgnore: MutableSet<Int>
    val remoteEventIdsToIgnore: MutableSet<Int>
}

class InMemorySynchronizerStateStorage : SynchronizerStateStorage {
    override val lastLocalRevisions = HashMap<String, Int?>().withDefault { null }
    override val lastRemoteRevisions = HashMap<String, Int?>().withDefault { null }
    override val localEventIdsToIgnore = HashSet<Int>()
    override val remoteEventIdsToIgnore = HashSet<Int>()
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
    override val localEventIdsToIgnore = database
            .hashSet("localEventIdsToIgnore", Serializer.INTEGER)
            .createOrOpen()
    override val remoteEventIdsToIgnore = database
            .hashSet("remoteEventIdsToIgnore", Serializer.INTEGER)
            .createOrOpen()
}
