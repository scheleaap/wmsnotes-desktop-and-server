package info.maaskant.wmsnotes.client.synchronization

import info.maaskant.wmsnotes.model.CommandOrigin
import info.maaskant.wmsnotes.model.Path
import info.maaskant.wmsnotes.model.folder.CreateFolderCommand
import info.maaskant.wmsnotes.model.note.CreateNoteCommand
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

internal class CommandToCommandRequestMapperTest {
    private val lastRevision = 11

    @Test
    fun noteCommand() {
        // Given
        val command = CreateNoteCommand(aggId = "agg", path = Path("el"), title = "Title", content = "Text")
        val origin = randomOrigin()
        val mapper = CommandToCommandRequestMapper()

        // When
        val request = mapper.map(command, lastRevision, origin)

        // Then
        assertThat(request.aggId).isEqualTo(command.aggId)
        assertThat(request.commands).isEqualTo(listOf(command))
        assertThat(request.lastRevision).isEqualTo(lastRevision)
        assertThat(request.requestId).isNotEqualTo(0)
        assertThat(request.origin).isEqualTo(origin)
    }

    @Test
    fun folderCommand() {
        // Given
        val command = CreateFolderCommand(path = Path("el"))
        val origin = randomOrigin()
        val mapper = CommandToCommandRequestMapper()

        // When
        val request = mapper.map(command, lastRevision, origin)

        // Then
        assertThat(request.aggId).isEqualTo(command.aggId)
        assertThat(request.commands).isEqualTo(listOf(command))
        assertThat(request.lastRevision).isEqualTo(lastRevision)
        assertThat(request.requestId).isNotEqualTo(0)
        assertThat(request.origin).isEqualTo(origin)
    }

    private fun randomOrigin(): CommandOrigin {
        val values = CommandOrigin.values()
        return values[Random.nextInt(values.size)]
    }
}