package info.maaskant.wmsnotes.model.folder

import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.Path

sealed class FolderCommand(aggId: String) : Command(aggId)

data class CreateFolderCommand(val path: Path) : FolderCommand(Folder.aggId(path))
data class DeleteFolderCommand(val path: Path) : FolderCommand(Folder.aggId(path))