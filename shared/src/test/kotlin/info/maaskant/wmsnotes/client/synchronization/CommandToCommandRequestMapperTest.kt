package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.folder.CreateFolderCommand
import info.maaskant.wmsnotes.model.note.CreateNoteCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CommandToCommandRequestMapperTest {
    private val lastRevision = 11

    @Test
    fun noteCommand() {
        // Given
        val command = CreateNoteCommand(aggId = "agg", path = Path("el"), title = "Title", content = "Text")
        val mapper = CommandToCommandRequestMapper()

        // When
        val request = mapper.map(command, lastRevision)

        // Then
        assertThat(request.aggId).isEqualTo(command.aggId)
        assertThat(request.commands).isEqualTo(listOf(command))
        assertThat(request.lastRevision).isEqualTo(lastRevision)
        assertThat(request.requestId).isNotEqualTo(0)
    }

    @Test
    fun folderCommand() {
        // Given
        val command = CreateFolderCommand(path = Path("el"))
        val mapper = CommandToCommandRequestMapper()

        // When
        val request = mapper.map(command, lastRevision)

        // Then
        assertThat(request.aggId).isEqualTo(command.aggId)
        assertThat(request.commands).isEqualTo(listOf(command))
        assertThat(request.lastRevision).isEqualTo(lastRevision)
        assertThat(request.requestId).isNotEqualTo(0)
    }
}