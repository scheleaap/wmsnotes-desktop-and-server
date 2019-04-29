@file:JvmName("Utils")

package info.maaskant.wmsnotes.model.note.policy

fun extractTitleFromContent(content: String): String? =
        content.lineSequence()
                .filter { it.isNotBlank() }
                .map { if (it.startsWith("# ")) it.substring(2) else it }
                .map { it.trim() }
                .firstOrNull()
