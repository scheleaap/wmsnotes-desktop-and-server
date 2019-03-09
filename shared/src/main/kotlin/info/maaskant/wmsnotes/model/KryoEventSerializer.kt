package info.maaskant.wmsnotes.model

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.model.folder.FolderCreatedEvent
import info.maaskant.wmsnotes.model.folder.FolderCreatedEventSerializer
import info.maaskant.wmsnotes.model.folder.FolderDeletedEvent
import info.maaskant.wmsnotes.model.folder.FolderDeletedEventSerializer
import info.maaskant.wmsnotes.model.note.*
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializer

class KryoEventSerializer(kryoPool: Pool<Kryo>) : KryoSerializer<Event>(
        kryoPool,
        Registration(NoteCreatedEvent::class.java, NoteCreatedEventSerializer(), 11),
        Registration(NoteDeletedEvent::class.java, NoteDeletedEventSerializer(), 12),
        Registration(NoteUndeletedEvent::class.java, NoteUndeletedEventSerializer(), 13),
        Registration(AttachmentAddedEvent::class.java, AttachmentAddedEventSerializer(), 14),
        Registration(AttachmentDeletedEvent::class.java, AttachmentDeletedEventSerializer(), 15),
        Registration(ContentChangedEvent::class.java, ContentChangedEventSerializer(), 16),
        Registration(TitleChangedEvent::class.java, TitleChangedEventSerializer(), 17),
        Registration(MovedEvent::class.java, MovedEventSerializer(), 18),
        Registration(FolderCreatedEvent::class.java, FolderCreatedEventSerializer(), 19),
        Registration(FolderDeletedEvent::class.java, FolderDeletedEventSerializer(), 20)
)
