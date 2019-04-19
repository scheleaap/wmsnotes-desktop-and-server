package info.maaskant.wmsnotes.model.folder

import info.maaskant.wmsnotes.model.AggregateCommand
import info.maaskant.wmsnotes.model.Path

sealed class FolderCommand(aggId: String) : AggregateCommand(aggId)

data class CreateFolderCommand(val path: Path) : FolderCommand(Folder.aggId(path))
data class DeleteFolderCommand(val path: Path, val lastRevision: Int) : FolderCommand(Folder.aggId(path))