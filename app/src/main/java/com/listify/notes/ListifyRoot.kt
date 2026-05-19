package com.listify.notes

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listify.notes.data.NoteRepository
import com.listify.notes.domain.MarkdownAction
import com.listify.notes.domain.MarkdownFormatter
import com.listify.notes.domain.Note
import com.listify.notes.domain.NoteColor
import com.listify.notes.ui.navigation.ListifyDestination
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ListifyRoot() {
    val context = LocalContext.current
    val repository = remember { NoteRepository(context.applicationContext) }
    val notes by repository.notes.collectAsState()
    var destination by remember { mutableStateOf(ListifyDestination.Notes) }
    var openedNote by remember { mutableStateOf<Note?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var backupText by remember { mutableStateOf("") }

    LaunchedEffect(repository) {
        repository.seedIfEmpty()
    }

    openedNote?.let { note ->
        if (isEditing) {
            NoteEditor(
                note = note,
                onBack = { isEditing = false },
                onSave = {
                    repository.saveNote(it)
                    openedNote = it
                    isEditing = false
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                },
                onTrash = {
                    repository.moveToTrash(note.id)
                    openedNote = null
                    isEditing = false
                }
            )
        } else {
            NoteReader(
                note = notes.firstOrNull { it.id == note.id } ?: note,
                onBack = { openedNote = null },
                onEdit = { isEditing = true },
                onTrash = {
                    repository.moveToTrash(note.id)
                    openedNote = null
                }
            )
        }
        return
    }

    Scaffold(
        topBar = {
            ListifyTopBar(
                destination = destination,
                notes = notes,
                onExport = {
                    backupText = repository.exportBackup()
                    destination = ListifyDestination.Settings
                }
            )
        },
        bottomBar = {
            NavigationBar {
                ListifyDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == destination,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (destination == ListifyDestination.Notes) {
                FloatingActionButton(
                    onClick = {
                        openedNote = repository.createNote()
                        isEditing = false
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New note")
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (destination) {
                ListifyDestination.Notes -> NotesScreen(
                    notes = notes.filterNot { it.isTrashed || it.isArchived },
                    onOpen = { openedNote = it },
                    onPin = { repository.togglePinned(it.id) },
                    onArchive = { repository.toggleArchived(it.id) },
                    onTrash = { repository.moveToTrash(it.id) }
                )

                ListifyDestination.Folders -> FoldersScreen(notes = notes.filterNot { it.isTrashed }, onOpen = { openedNote = it })
                ListifyDestination.Search -> SearchScreen(notes = notes.filterNot { it.isTrashed }, onOpen = { openedNote = it })
                ListifyDestination.Reminders -> RemindersScreen(
                    notes = notes.filterNot { it.isTrashed },
                    onOpen = { openedNote = it },
                    onSetReminder = { repository.setReminderTomorrow(it.id) },
                    onClearReminder = { repository.clearReminder(it.id) }
                )

                ListifyDestination.Settings -> SettingsScreen(
                    notes = notes,
                    backupText = backupText,
                    onBackupTextChange = { backupText = it },
                    onExport = { backupText = repository.exportBackup() },
                    onImport = {
                        val imported = repository.importBackup(backupText)
                        Toast.makeText(context, if (imported) "Imported" else "Invalid backup", Toast.LENGTH_SHORT).show()
                    },
                    onRestore = { repository.restore(it.id) },
                    onDeleteForever = { repository.deleteForever(it.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListifyTopBar(destination: ListifyDestination, notes: List<Note>, onExport: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text("Listify", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${notes.count { !it.isTrashed }} notes on this device",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            if (destination == ListifyDestination.Settings) {
                TextButton(onClick = onExport) { Text("Export") }
            }
        }
    )
}

@Composable
private fun NotesScreen(
    notes: List<Note>,
    onOpen: (Note) -> Unit,
    onPin: (Note) -> Unit,
    onArchive: (Note) -> Unit,
    onTrash: (Note) -> Unit
) {
    if (notes.isEmpty()) {
        EmptyState("No active notes")
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                onOpen = { onOpen(note) },
                actions = {
                    IconButton(onClick = { onPin(note) }) {
                        Icon(if (note.isPinned) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = "Pin")
                    }
                    IconButton(onClick = { onArchive(note) }) { Icon(Icons.Default.Archive, contentDescription = "Archive") }
                    IconButton(onClick = { onTrash(note) }) { Icon(Icons.Default.Delete, contentDescription = "Trash") }
                }
            )
        }
    }
}

@Composable
private fun FoldersScreen(notes: List<Note>, onOpen: (Note) -> Unit) {
    val folders = notes.groupBy { it.folder.ifBlank { "Personal" } }.toSortedMap()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        folders.forEach { (folder, folderNotes) ->
            item(key = "folder-$folder") {
                Text(folder, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    folderNotes.take(4).forEach { note ->
                        CompactNoteRow(note = note, onClick = { onOpen(note) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(notes: List<Note>, onOpen: (Note) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = notes.filter { note ->
        val haystack = "${note.title} ${note.body} ${note.folder} ${note.tags.joinToString(" ")}"
        query.isBlank() || haystack.contains(query, ignoreCase = true)
    }

    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search") },
            singleLine = true,
            leadingIcon = { Icon(ListifyDestination.Search.icon, contentDescription = null) }
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(results, key = { it.id }) { note ->
                CompactNoteRow(note = note, onClick = { onOpen(note) })
            }
        }
    }
}

@Composable
private fun RemindersScreen(
    notes: List<Note>,
    onOpen: (Note) -> Unit,
    onSetReminder: (Note) -> Unit,
    onClearReminder: (Note) -> Unit
) {
    val reminderNotes = notes.filter { it.reminderAt != null }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Reminders", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("Local reminders are kept with your notes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(notes, key = { it.id }) { note ->
            Card(colors = CardDefaults.cardColors(containerColor = noteColor(note.color))) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(note) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(note.displayTitle, fontWeight = FontWeight.SemiBold)
                        Text(
                            note.reminderAt?.let { "Due ${formatDate(it)}" } ?: "No reminder",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (note.reminderAt == null) {
                        OutlinedButton(onClick = { onSetReminder(note) }) { Text("Tomorrow") }
                    } else {
                        TextButton(onClick = { onClearReminder(note) }) { Text("Clear") }
                    }
                }
            }
        }
        if (reminderNotes.isEmpty()) {
            item { EmptyState("No reminders set") }
        }
    }
}

@Composable
private fun SettingsScreen(
    notes: List<Note>,
    backupText: String,
    onBackupTextChange: (String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onRestore: (Note) -> Unit,
    onDeleteForever: (Note) -> Unit
) {
    val trashed = notes.filter { it.isTrashed }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Backup", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onExport) { Text("Export") }
                OutlinedButton(onClick = onImport) { Text("Import") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = backupText,
                onValueChange = onBackupTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                label = { Text("Backup JSON") }
            )
        }
        item {
            Divider()
            Spacer(Modifier.height(12.dp))
            Text("Trash", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        items(trashed, key = { it.id }) { note ->
            Card {
                Column(Modifier.padding(14.dp)) {
                    Text(note.displayTitle, fontWeight = FontWeight.SemiBold)
                    Text(note.preview, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row {
                        TextButton(onClick = { onRestore(note) }) { Text("Restore") }
                        TextButton(onClick = { onDeleteForever(note) }) { Text("Delete") }
                    }
                }
            }
        }
        if (trashed.isEmpty()) {
            item { Text("Trash is empty", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun NoteReader(note: Note, onBack: () -> Unit, onEdit: () -> Unit, onTrash: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(note.displayTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onTrash) { Icon(Icons.Default.Delete, contentDescription = "Trash") }
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Text(
                    text = note.displayTitle,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = noteColor(note.color),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = note.body.ifBlank { "No markdown yet." },
                        modifier = Modifier.padding(16.dp),
                        fontSize = 17.sp,
                        lineHeight = 25.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun NoteEditor(note: Note, onBack: () -> Unit, onSave: (Note) -> Unit, onTrash: () -> Unit) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(note.body) }
    var folder by remember(note.id) { mutableStateOf(note.folder) }
    var tags by remember(note.id) { mutableStateOf(note.tags.joinToString(", ")) }
    var color by remember(note.id) { mutableStateOf(note.color) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit note") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onTrash) { Icon(Icons.Default.Delete, contentDescription = "Trash") }
                    IconButton(
                        onClick = {
                            onSave(
                                note.copy(
                                    title = title,
                                    body = body,
                                    folder = folder.ifBlank { "Personal" },
                                    tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                    color = color
                                )
                            )
                        }
                    ) { Icon(Icons.Default.Save, contentDescription = "Save") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    singleLine = true
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MarkdownAction.entries.forEach { action ->
                        OutlinedButton(onClick = { body = MarkdownFormatter.apply(action, body) }) {
                            Text(action.name.take(1))
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    label = { Text("Markdown") }
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    NoteColor.entries.forEach { item ->
                        Box(
                            modifier = Modifier
                                .size(if (item == color) 34.dp else 28.dp)
                                .clip(CircleShape)
                                .background(noteColor(item))
                                .clickable { color = item }
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = folder,
                        onValueChange = { folder = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Folder") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Tags") },
                        singleLine = true
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, onOpen: () -> Unit, actions: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = noteColor(note.color)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpen)
                ) {
                    Text(note.displayTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    if (note.preview.isNotBlank()) {
                        Text(note.preview, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(8.dp))
                    NoteMeta(note)
                }
                Row { actions() }
            }
        }
    }
}

@Composable
private fun CompactNoteRow(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = noteColor(note.color)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(note.displayTitle, fontWeight = FontWeight.SemiBold)
                Text(note.preview.ifBlank { note.folder }, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun NoteMeta(note: Note) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MetaPill(note.folder)
        note.tags.take(3).forEach { MetaPill("#$it") }
        note.reminderAt?.let { MetaPill(formatDate(it)) }
    }
}

@Composable
private fun MetaPill(label: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 12.sp)
    }
}

@Composable
private fun EmptyState(title: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun noteColor(color: NoteColor): Color {
    return when (color) {
        NoteColor.Paper -> Color(0xFFFFFDF8)
        NoteColor.Sage -> Color(0xFFE8F3EC)
        NoteColor.Sky -> Color(0xFFE6F0F8)
        NoteColor.Rose -> Color(0xFFF8E8E8)
        NoteColor.Sun -> Color(0xFFFFF3D8)
    }
}

private fun formatDate(timeMillis: Long): String {
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timeMillis))
}
