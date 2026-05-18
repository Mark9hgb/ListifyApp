# Offline Notes App Design

## Summary

Build a native Android notes app using Kotlin, Jetpack Compose, Material 3, Room, and local device storage. The app is fully offline with no accounts, no cloud sync, and no network dependency. It should feel like a polished warm journal while still supporting advanced organization, search, privacy, reminders, attachments, and backup workflows.

## Goals

- Provide a complete offline notes workflow for writing, organizing, finding, protecting, and backing up notes.
- Use a native Android architecture that supports long-term maintainability and strong Material UX.
- Store all primary data locally and keep user control over backup/export.
- Make the first release feature-rich without adding cloud, account, or sync complexity.

## Non-Goals

- No user accounts.
- No cloud sync.
- No web app, iOS app, or cross-platform runtime in the first version.
- No collaborative editing.

## Technology

- Language: Kotlin.
- UI: Jetpack Compose with Material 3.
- Architecture: MVVM-style presentation layer with repositories/use cases where useful.
- Database: Room over SQLite.
- Search: SQLite FTS for note title and body search.
- Security: Android Keystore and BiometricPrompt/device credential for app lock and locked notes.
- Notifications: local Android notifications for reminders.
- Storage: app-controlled local files for attachments and backup archives.

## Navigation And Visual Direction

Use bottom navigation as the primary app structure:

1. Notes: all notes, pinned notes, recent notes, and quick capture.
2. Folders: notebook-style organization.
3. Search: full-text search, filters, tags, colors, and date filters.
4. Reminders: upcoming and overdue note reminders.
5. Settings: app lock, export/import, theme, backup, and trash.

The UI direction is warm journal Material 3:

- Soft neutral surfaces with restrained accent colors.
- Note color labels that help scanning without overwhelming the screen.
- 8px card radius unless a native Material component requires otherwise.
- Practical spacing and dense but readable note lists.
- No marketing-style landing screen; the first screen is the actual notes workspace.

## Core Features

### Notes

- Create, edit, duplicate, archive, restore, and delete notes.
- Pin important notes.
- Move notes to trash before permanent deletion.
- Support compact list and grid views.
- Support swipe actions and multi-select bulk actions.
- Autosave note edits locally and show clear saved/unsaved state.

### Editor

- Use a hybrid Markdown editor.
- Store note content as Markdown text.
- Provide toolbar actions for headings, bold, italic, links, inline code, code blocks, quotes, checklists, and bullet lists.
- Include edit mode, live preview mode, and read mode.
- Render Markdown consistently in preview, read mode, and note detail surfaces.

### Organization

- Folders for broad grouping.
- Tags for cross-folder classification.
- Note colors for visual grouping.
- Archive for notes that should stay searchable but out of the main list.
- Trash for recoverable deletion.

### Search And Filters

- Full-text local search over title and body.
- Filters for folder, tag, color, pinned state, archived state, reminder state, locked state visibility, and date ranges.
- Search results should hide private note body previews until the app is unlocked.

### Reminders

- Local reminders only.
- Support one-time reminders in the first version.
- Include upcoming and overdue reminder views.
- Handle missing notification permission with a clear in-app state and settings action.

### Attachments

- Attach images and files to notes.
- Copy attachments into app-controlled local storage.
- Store attachment metadata in Room.
- Show recoverable errors if an attachment cannot be copied or opened.

### Privacy

- App lock using biometrics or device credential.
- Locked/private notes require unlock before opening.
- Locked note bodies and previews are hidden in lists and search results until unlocked.
- Normal notes remain unencrypted in Room for fast search and simple backup.
- Locked note body content is encrypted with a Keystore-backed key and is excluded from body full-text search while locked.

### Backup And Import

- Export notes, folders, tags, reminders, and attachment metadata to a structured local archive.
- Include attachment files in the archive.
- Support password-protected encrypted export.
- Import should validate archive structure, detect conflicts, and report partial failures clearly.

## Data Model

Use Room entities with clear boundaries:

- `Note`: title, Markdown body, preview text, folder id, color, pinned flag, archived flag, trash flag, locked flag, created timestamp, updated timestamp, deleted timestamp.
- `Folder`: name, sort order, optional color.
- `Tag`: name and color.
- `NoteTag`: many-to-many link between notes and tags.
- `Reminder`: note id, scheduled time, fired/completed state.
- `Attachment`: note id, local URI/path, MIME type, display name, size.
- `AppSettings`: theme, lock settings, editor preferences, and backup preferences.

Use SQLite FTS tables for searchable normal note title/body content. Keep FTS updates tied to note write operations so search stays accurate. Locked notes can expose title-only search metadata while their encrypted body stays out of FTS.

## Key Flows

### Quick Capture

The floating add button opens a new note immediately. The note autosaves as the user types. Empty unsaved notes can be discarded automatically when leaving the editor.

### Find A Note

The user opens Search, types a query, and can refine results by folder, tag, color, pinned state, archived state, reminder state, and date range. Locked note previews stay hidden until unlock.

### Lock A Note

The user marks a note as locked. Opening that note later requires biometric or device credential unlock. If unlock fails or is cancelled, the app returns to the previous safe screen.

### Backup

The user exports a backup from Settings. The app prepares metadata and attachments, optionally encrypts the archive with a password, and writes it to a user-selected location. Import validates before applying changes.

## Error Handling

Handle these cases explicitly:

- Attachment copy or open failure.
- Backup export/import failure.
- Invalid or unsupported backup file.
- Notification permission missing.
- Biometric unavailable or authentication cancelled.
- Markdown rendering failure for malformed input.
- Database migration failure.

Errors should be local, recoverable, and written in plain language. Destructive actions should have undo or confirmation where appropriate.

## Testing

Test coverage should focus on behavior and migration safety:

- Room DAO behavior and migrations.
- Markdown parsing and rendering.
- Search and filter combinations.
- Backup export/import round trip.
- Reminder scheduling logic.
- Locked-note visibility rules.
- Compose UI flows for create, edit, search, restore, and unlock.

## Acceptance Criteria

- The app launches directly into the notes workspace.
- Users can create and edit Markdown notes offline.
- Users can organize notes with folders, tags, colors, pinning, archive, and trash.
- Users can search note content locally and filter results.
- Users can schedule local reminders.
- Users can attach local files/images to notes.
- Users can lock the app and open private notes only after unlock.
- Users can export and import a local backup archive.
- The UI follows the approved warm journal Material 3 direction with bottom navigation.
