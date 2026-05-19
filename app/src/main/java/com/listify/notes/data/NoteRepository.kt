package com.listify.notes.data

import android.content.Context
import com.listify.notes.domain.Note
import com.listify.notes.domain.NoteColor
import com.listify.notes.domain.NotesBackup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NoteRepository(context: Context) {
    private val prefs = context.getSharedPreferences("listify_notes", Context.MODE_PRIVATE)
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val _notes = MutableStateFlow(loadNotes())

    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    fun createNote(): Note {
        val now = System.currentTimeMillis()
        val note = Note(
            id = now,
            title = "",
            body = "",
            createdAt = now,
            updatedAt = now
        )
        updateNotes(listOf(note) + _notes.value)
        return note
    }

    fun saveNote(note: Note) {
        updateNotes(_notes.value.map {
            if (it.id == note.id) note.copy(updatedAt = System.currentTimeMillis()) else it
        })
    }

    fun togglePinned(noteId: Long) {
        updateNote(noteId) { it.copy(isPinned = !it.isPinned) }
    }

    fun toggleArchived(noteId: Long) {
        updateNote(noteId) { it.copy(isArchived = !it.isArchived) }
    }

    fun setReminderTomorrow(noteId: Long) {
        val tomorrow = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
        updateNote(noteId) { it.copy(reminderAt = tomorrow) }
    }

    fun clearReminder(noteId: Long) {
        updateNote(noteId) { it.copy(reminderAt = null) }
    }

    fun moveToTrash(noteId: Long) {
        updateNote(noteId) { it.copy(isTrashed = true, isArchived = false) }
    }

    fun restore(noteId: Long) {
        updateNote(noteId) { it.copy(isTrashed = false) }
    }

    fun deleteForever(noteId: Long) {
        updateNotes(_notes.value.filterNot { it.id == noteId })
    }

    fun exportBackup(): String {
        return json.encodeToString(NotesBackup(exportedAt = System.currentTimeMillis(), notes = _notes.value))
    }

    fun importBackup(raw: String): Boolean {
        return runCatching {
            val backup = json.decodeFromString<NotesBackup>(raw)
            if (backup.schemaVersion != 1) return false
            updateNotes(backup.notes.sortedWith(noteSort))
            true
        }.getOrDefault(false)
    }

    fun seedIfEmpty() {
        if (_notes.value.isNotEmpty()) return
        val now = System.currentTimeMillis()
        updateNotes(
            listOf(
                Note(
                    id = now,
                    title = "Welcome to Listify",
                    body = "Write notes, pin important ideas, search offline, and keep everything on this device.",
                    folder = "Personal",
                    color = NoteColor.Sage,
                    tags = listOf("start"),
                    isPinned = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
        )
    }

    private fun updateNote(noteId: Long, transform: (Note) -> Note) {
        updateNotes(_notes.value.map { note ->
            if (note.id == noteId) transform(note).copy(updatedAt = System.currentTimeMillis()) else note
        })
    }

    private fun updateNotes(notes: List<Note>) {
        val sorted = notes.sortedWith(noteSort)
        _notes.value = sorted
        prefs.edit().putString(NOTES_KEY, json.encodeToString(sorted)).apply()
    }

    private fun loadNotes(): List<Note> {
        val raw = prefs.getString(NOTES_KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Note>>(raw).sortedWith(noteSort) }.getOrDefault(emptyList())
    }

    private companion object {
        const val NOTES_KEY = "notes_json"

        val noteSort = compareByDescending<Note> { it.isPinned }
            .thenByDescending { it.updatedAt }
    }
}
