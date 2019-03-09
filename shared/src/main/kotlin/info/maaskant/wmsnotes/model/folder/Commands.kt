package info.maaskant.wmsnotes.model.folder

import info.maaskant.wmsnotes.model.Command
import info.maaskant.wmsnotes.model.Path

sealed class FolderCommand : Command

data class CreateFolderCommand(val path: Path, val lastRevision: Int) : FolderCommand()
data class DeleteFolderCommand(val path: Path, val lastRevision: Int) : FolderCommand()
