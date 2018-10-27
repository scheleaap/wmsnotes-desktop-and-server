package info.maaskant.wmsnotes.desktop.app

import dagger.Module
import dagger.Provides
import info.maaskant.wmsnotes.client.indexing.NoteIndex
import info.maaskant.wmsnotes.desktop.app.Configuration.storeInMemory
import info.maaskant.wmsnotes.model.eventstore.EventStore
import io.reactivex.schedulers.Schedulers
import org.mapdb.DB
import org.mapdb.DBMaker
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
class IndexingModule {

    @Singleton
    @Provides
    @IndexDatabase
    fun mapDbIndexDatabase(): DB = if (storeInMemory) {
        DBMaker.memoryDB()
                .closeOnJvmShutdown()
                .make()
    } else {
        val file = File("desktop_data/indices.db")
        file.parentFile.mkdirs()
        DBMaker.fileDB(file)
                .fileMmapEnableIfSupported()
                .closeOnJvmShutdown()
                .make()
    }

    @Singleton
    @Provides
    fun noteIndex(eventStore: EventStore, @IndexDatabase database: DB): NoteIndex =
            NoteIndex(eventStore, database, Schedulers.io())

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class IndexDatabase

}