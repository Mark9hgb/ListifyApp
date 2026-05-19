package com.listify.notes.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class ListifyDestination(val label: String, val icon: ImageVector) {
    Notes("Notes", Icons.Default.Note),
    Folders("Folders", Icons.Default.Folder),
    Search("Search", Icons.Default.Search),
    Reminders("Reminders", Icons.Default.Alarm),
    Settings("Settings", Icons.Default.Settings)
}
