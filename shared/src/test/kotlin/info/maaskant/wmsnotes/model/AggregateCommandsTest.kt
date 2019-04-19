package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.folder.CreateFolderCommand
import info.maaskant.wmsnotes.model.folder.DeleteFolderCommand
import info.maaskant.wmsnotes.model.note.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class AggregateCommandsTest {
    private val aggId = "note"
    private val path = Path("el1", "el2")
    private val title = "Title"
    private val content = "Text"

    @Test
    fun `equals and hashCode for shared fields`() {
        val o = DeleteNoteCommand(aggId = aggId, lastRevision = 1)
        val different1 = DeleteNoteCommand(aggId = "different", lastRevision = 1)

        assertThat(o).isEqualTo(o)
        assertThat(o.hashCode()).isEqualTo(o.hashCode())
        assertThat(o).isNotEqualTo(different1)
        assertThat(o.hashCode()).isNotEqualTo(different1.hashCode())
    }

    @TestFactory
    fun `equals and hashCode`(): List<DynamicTest> {
        return listOf(
                Item(
                        o = CreateNoteCommand(aggId = aggId, path = path, title = title, content = content),
                        sameButCopy = CreateNoteCommand(aggId = aggId, path = path, title = title, content = content),
                        differents = listOf(
                                CreateNoteCommand(aggId = aggId, path = Path("different"), title = title, content = content),
                                CreateNoteCommand(aggId = aggId, path = path, title = "Different", content = content),
                                CreateNoteCommand(aggId = aggId, path = path, title = title, content = "Different")
                        )
                ),
                Item(
                        o = DeleteNoteCommand(aggId = aggId, lastRevision = 1),
                        sameButCopy = DeleteNoteCommand(aggId = aggId, lastRevision = 1),
                        differents = listOf(
                                DeleteNoteCommand(aggId = "different", lastRevision = 1),
                                DeleteNoteCommand(aggId = aggId, lastRevision = 2)
                        )
                ),
                Item(
                        o = UndeleteNoteCommand(aggId = aggId, lastRevision = 1),
                        sameButCopy = UndeleteNoteCommand(aggId = aggId, lastRevision = 1),
                        differents = listOf(
                                UndeleteNoteCommand(aggId = "different", lastRevision = 1),
                                UndeleteNoteCommand(aggId = aggId, lastRevision = 2)
                        )
                ),
                Item(
                        o = AddAttachmentCommand(aggId = aggId, lastRevision = 1, name = "att-1", content = "data".toByteArray()),
                        sameButCopy = AddAttachmentCommand(aggId = aggId, lastRevision = 1, name = "att-1", content = "data".toByteArray()),
                        differents = listOf(
                                AddAttachmentCommand(aggId = aggId, lastRevision = 2, name = "att-1", content = "data".toByteArray()),
                                AddAttachmentCommand(aggId = aggId, lastRevision = 1, name = "different", content = "data".toByteArray()),
                                AddAttachmentCommand(aggId = aggId, lastRevision = 1, name = "att-1", content = "different".toByteArray())
                        )
                ),
                Item(
                        o = DeleteAttachmentCommand(aggId = aggId, lastRevision = 1, name = "att-1"),
                        sameButCopy = DeleteAttachmentCommand(aggId = aggId, lastRevision = 1, name = "att-1"),
                        differents = listOf(
                                DeleteAttachmentCommand(aggId = aggId, lastRevision = 2, name = "att-1"),
                                DeleteAttachmentCommand(aggId = aggId, lastRevision = 1, name = "different")
                        )
                ),
                Item(
                        o = ChangeContentCommand(aggId = aggId, lastRevision = 1, content = content),
                        sameButCopy = ChangeContentCommand(aggId = aggId, lastRevision = 1, content = content),
                        differents = listOf(
                                ChangeContentCommand(aggId = aggId, lastRevision = 2, content = content),
                                ChangeContentCommand(aggId = aggId, lastRevision = 1, content = "Different")
                        )
                ),
                Item(
                        o = ChangeTitleCommand(aggId = aggId, lastRevision = 1, title = title),
                        sameButCopy = ChangeTitleCommand(aggId = aggId, lastRevision = 1, title = title),
                        differents = listOf(
                                ChangeTitleCommand(aggId = aggId, lastRevision = 2, title = title),
                                ChangeTitleCommand(aggId = aggId, lastRevision = 1, title = "Different")
                        )
                ),
                Item(
                        o = MoveCommand(aggId = aggId, lastRevision = 1, path = path),
                        sameButCopy = MoveCommand(aggId = aggId, lastRevision = 1, path = path),
                        differents = listOf(
                                MoveCommand(aggId = aggId, lastRevision = 2, path = path),
                                MoveCommand(aggId = aggId, lastRevision = 1, path = Path("different"))
                        )
                ),
                Item(
                        o = CreateFolderCommand(path = path),
                        sameButCopy = CreateFolderCommand(path = path),
                        differents = listOf(CreateFolderCommand(path = Path("different")))
                ),
                Item(
                        o = DeleteFolderCommand(path = path, lastRevision = 1),
                        sameButCopy = DeleteFolderCommand(path = path, lastRevision = 1),
                        differents = listOf(
                                DeleteFolderCommand(path = Path("different"), lastRevision = 1),
                                DeleteFolderCommand(path = path, lastRevision = 2)
                        )
                )
                // Add more classes here
        ).map {
            DynamicTest.dynamicTest(it.o::class.simpleName) {
                assertThat(it.o).isEqualTo(it.o)
                assertThat(it.o.hashCode()).isEqualTo(it.o.hashCode())
                assertThat(it.o).isEqualTo(it.sameButCopy)
                assertThat(it.o.hashCode()).isEqualTo(it.sameButCopy.hashCode())
                for (different in it.differents) {
                    assertThat(it.o).isNotEqualTo(different)
                    assertThat(it.o.hashCode()).isNotEqualTo(different.hashCode())
                }
            }
        }
    }

    private data class Item(val o: AggregateCommand, val sameButCopy: AggregateCommand, val differents: List<AggregateCommand>)
}
