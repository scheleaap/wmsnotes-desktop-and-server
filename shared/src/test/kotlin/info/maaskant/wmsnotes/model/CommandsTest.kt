package info.maaskant.wmsnotes.model

import info.maaskant.wmsnotes.model.folder.CreateFolderCommand
import info.maaskant.wmsnotes.model.folder.DeleteFolderCommand
import info.maaskant.wmsnotes.model.note.*
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class CommandsTest {
    private val aggId = "note"
    private val path = Path("el1", "el2")
    private val title = "Title"
    private val content = "Text"

    @Test
    fun `equals and hashCode for shared fields`() {
        val o = DeleteNoteCommand(aggId = aggId)
        val different1 = DeleteNoteCommand(aggId = "different")

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
                        o = DeleteNoteCommand(aggId = aggId),
                        sameButCopy = DeleteNoteCommand(aggId = aggId),
                        differents = listOf(
                                DeleteNoteCommand(aggId = "different")
                        )
                ),
                Item(
                        o = UndeleteNoteCommand(aggId = aggId),
                        sameButCopy = UndeleteNoteCommand(aggId = aggId),
                        differents = listOf(
                                UndeleteNoteCommand(aggId = "different")
                        )
                ),
                Item(
                        o = AddAttachmentCommand(aggId = aggId, name = "att-1", content = "data".toByteArray()),
                        sameButCopy = AddAttachmentCommand(aggId = aggId, name = "att-1", content = "data".toByteArray()),
                        differents = listOf(
                                AddAttachmentCommand(aggId = aggId, name = "different", content = "data".toByteArray()),
                                AddAttachmentCommand(aggId = aggId, name = "att-1", content = "different".toByteArray())
                        )
                ),
                Item(
                        o = DeleteAttachmentCommand(aggId = aggId, name = "att-1"),
                        sameButCopy = DeleteAttachmentCommand(aggId = aggId, name = "att-1"),
                        differents = listOf(
                                DeleteAttachmentCommand(aggId = aggId, name = "different")
                        )
                ),
                Item(
                        o = ChangeContentCommand(aggId = aggId, content = content),
                        sameButCopy = ChangeContentCommand(aggId = aggId, content = content),
                        differents = listOf(
                                ChangeContentCommand(aggId = aggId, content = "Different")
                        )
                ),
                Item(
                        o = ChangeTitleCommand(aggId = aggId, title = title),
                        sameButCopy = ChangeTitleCommand(aggId = aggId, title = title),
                        differents = listOf(
                                ChangeTitleCommand(aggId = aggId, title = "Different")
                        )
                ),
                Item(
                        o = MoveCommand(aggId = aggId, path = path),
                        sameButCopy = MoveCommand(aggId = aggId, path = path),
                        differents = listOf(
                                MoveCommand(aggId = aggId, path = Path("different"))
                        )
                ),
                Item(
                        o = CreateFolderCommand(path = path),
                        sameButCopy = CreateFolderCommand(path = path),
                        differents = listOf(CreateFolderCommand(path = Path("different")))
                ),
                Item(
                        o = DeleteFolderCommand(path = path),
                        sameButCopy = DeleteFolderCommand(path = path),
                        differents = listOf(
                                DeleteFolderCommand(path = Path("different")
                                )
                        )
                        // Add more classes here
                )
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

    private data class Item(val o: Command, val sameButCopy: Command, val differents: List<Command>)
}
