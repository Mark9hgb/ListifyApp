package com.listify.notes.domain

enum class MarkdownAction {
    Heading,
    Bold,
    Checklist,
    Bullet,
    Quote
}

object MarkdownFormatter {
    fun apply(action: MarkdownAction, text: String): String {
        return when (action) {
            MarkdownAction.Heading -> prefixLine(text, "# ")
            MarkdownAction.Bold -> "$text****"
            MarkdownAction.Checklist -> prefixLine(text, "- [ ] ")
            MarkdownAction.Bullet -> prefixLine(text, "- ")
            MarkdownAction.Quote -> prefixLine(text, "> ")
        }
    }

    private fun prefixLine(text: String, prefix: String): String {
        return if (text.isBlank()) prefix else "$text\n$prefix"
    }
}
