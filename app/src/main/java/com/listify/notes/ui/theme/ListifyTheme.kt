package com.listify.notes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WarmJournalColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    secondary = Color(0xFF8A5A2B),
    surface = Color(0xFFFFFDF8),
    background = Color(0xFFFBFAF7),
    surfaceVariant = Color(0xFFEDE7DD),
    outline = Color(0xFF817568)
)

@Composable
fun ListifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WarmJournalColors,
        content = content
    )
}
