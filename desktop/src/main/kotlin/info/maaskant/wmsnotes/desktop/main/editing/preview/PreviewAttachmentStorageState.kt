package info.maaskant.wmsnotes.desktop.main.editing.preview

data class PreviewAttachmentStorageState(val storedAttachments: Map<String, String> = emptyMap()) {
    fun addAttachment(name: String, hash: String) = copy(storedAttachments = storedAttachments + (name to hash))
}