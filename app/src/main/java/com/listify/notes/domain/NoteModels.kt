package com.listify.notes.domain

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: Long,
    val title: String,
    val body: String,
    val folder: String = "Personal",
    val color: NoteColor = NoteColor.Paper,
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val reminderAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    val displayTitle: String
        get() = title.ifBlank { "Untitled" }

    val preview: String
        get() = body.lineSequence().firstOrNull { it.isNotBlank() }?.take(140).orEmpty()
}

@Serializable
enum class NoteColor(val label: String, val hex: Long) {
    Paper("Paper", 0xFFFFFDF8),
    Sage("Sage", 0xFFE8F3EC),
    Sky("Sky", 0xFFE6F0F8),
    Rose("Rose", 0xFFF8E8E8),
    Sun("Sun", 0xFFFFF3D8)
}

@Serializable
data class NotesBackup(
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val notes: List<Note>
)
