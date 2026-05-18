# Offline Notes App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable offline Android notes app with native Material 3 UI, local Room storage, hybrid Markdown editing, organization, search, reminders, attachments, privacy controls, and backup/import.

**Architecture:** Create a Kotlin + Jetpack Compose Android app from this currently empty repository. Use a small MVVM-style structure: `data` owns Room/file/security persistence, `domain` owns app models and use cases, and `ui` owns Compose screens/state. Keep the app offline-only by excluding auth, cloud, remote APIs, and network permissions.

**Tech Stack:** Kotlin, Android Gradle Plugin, Jetpack Compose Material 3, Room, Kotlin Serialization, WorkManager or AlarmManager for local reminders, AndroidX Biometric, Android Keystore, JUnit, Robolectric, and Compose UI tests.

---

## File Structure

Create this structure:

```text
/root/ListifyApp/
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  app/
    build.gradle.kts
    src/main/AndroidManifest.xml
    src/main/java/com/listify/notes/ListifyApp.kt
    src/main/java/com/listify/notes/MainActivity.kt
    src/main/java/com/listify/notes/data/db/
    src/main/java/com/listify/notes/data/files/
    src/main/java/com/listify/notes/data/backup/
    src/main/java/com/listify/notes/data/security/
    src/main/java/com/listify/notes/data/reminders/
    src/main/java/com/listify/notes/domain/
    src/main/java/com/listify/notes/ui/
    src/test/java/com/listify/notes/
    src/androidTest/java/com/listify/notes/
```

Responsibilities:

- `data/db`: Room entities, DAO interfaces, database, migrations, FTS search.
- `data/files`: local attachment copy/open helpers.
- `data/backup`: JSON archive models, export/import coordinator, encrypted archive handling.
- `data/security`: app lock state, locked note encryption, biometric integration boundary.
- `data/reminders`: local reminder scheduling and notification receiver.
- `domain`: plain Kotlin models, repository interfaces, and use cases.
- `ui`: Compose navigation, screens, state holders, theme, editor components.

---

### Task 1: Android Project Scaffold

**Files:**
- Create: `/root/ListifyApp/settings.gradle.kts`
- Create: `/root/ListifyApp/build.gradle.kts`
- Create: `/root/ListifyApp/gradle.properties`
- Create: `/root/ListifyApp/app/build.gradle.kts`
- Create: `/root/ListifyApp/app/src/main/AndroidManifest.xml`
- Create: `/root/ListifyApp/app/src/main/res/values/styles.xml`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/MainActivity.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/ListifyApp.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/theme/ListifyTheme.kt`

- [ ] **Step 1: Generate the Gradle wrapper**

Run:

```bash
gradle wrapper --gradle-version 8.14.1
```

Expected: `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.properties` exist.

- [ ] **Step 2: Create Gradle settings**

Add `/root/ListifyApp/settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ListifyApp"
include(":app")
```

- [ ] **Step 3: Create root build files**

Add `/root/ListifyApp/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") version "8.13.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
    id("com.google.devtools.ksp") version "2.2.21-2.0.4" apply false
}
```

Add `/root/ListifyApp/gradle.properties`:

```properties
android.useAndroidX=true
android.nonTransitiveRClass=true
android.defaults.buildfeatures.buildconfig=true
kotlin.code.style=official
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

- [ ] **Step 4: Create the app module build file**

Add `/root/ListifyApp/app/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.listify.notes"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.listify.notes"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val roomVersion = "2.8.3"

    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.ui:ui:1.9.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.4")
    debugImplementation("androidx.compose.ui:ui-tooling:1.9.4")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.6")

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("androidx.biometric:biometric:1.4.0-alpha02")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.11.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("org.robolectric:robolectric:4.16")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.9.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.9.4")
}
```

- [ ] **Step 5: Create the manifest, theme resource, and entry point**

Add `/root/ListifyApp/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".ListifyApp"
        android:allowBackup="false"
        android:label="Listify"
        android:theme="@style/Theme.Listify">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

Add `/root/ListifyApp/app/src/main/res/values/styles.xml`:

```xml
<resources>
    <style name="Theme.Listify" parent="android:style/Theme.Material.Light.NoActionBar">
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:navigationBarColor">#FFFDF8</item>
        <item name="android:statusBarColor">#FBFAF7</item>
    </style>
</resources>
```

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/ListifyApp.kt`:

```kotlin
package com.listify.notes

import android.app.Application

class ListifyApp : Application()
```

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/MainActivity.kt`:

```kotlin
package com.listify.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.listify.notes.ui.theme.ListifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ListifyTheme {
                ListifyRoot()
            }
        }
    }
}
```

- [ ] **Step 6: Create the first Compose root**

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/ListifyRoot.kt`:

```kotlin
package com.listify.notes

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListifyRoot() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Listify") }) }
    ) { padding ->
        Text(
            text = "Offline notes workspace",
            modifier = Modifier.padding(padding)
        )
    }
}
```

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/theme/ListifyTheme.kt`:

```kotlin
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
```

- [ ] **Step 7: Run the scaffold build**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradlew gradlew.bat gradle app
git commit -m "feat: scaffold Android notes app"
```

---

### Task 2: Domain Models And Room Storage

**Files:**
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/domain/Models.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/data/db/Entities.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/data/db/ListifyDatabase.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/data/db/NoteDao.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/data/db/NoteRepository.kt`
- Create: `/root/ListifyApp/app/src/test/java/com/listify/notes/data/db/NoteDaoTest.kt`

- [ ] **Step 1: Write Room DAO tests**

Add `/root/ListifyApp/app/src/test/java/com/listify/notes/data/db/NoteDaoTest.kt`:

```kotlin
package com.listify.notes.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NoteDaoTest {
    private lateinit var db: ListifyDatabase
    private lateinit var dao: NoteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ListifyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.noteDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertedNoteAppearsInActiveNotes() = runTest {
        val id = dao.upsertNote(NoteEntity(title = "Trip", bodyMarkdown = "Pack list"))

        val notes = dao.observeActiveNotes().first()

        assertEquals(id, notes.single().id)
        assertEquals("Trip", notes.single().title)
    }

    @Test
    fun trashedNoteIsHiddenFromActiveNotes() = runTest {
        val id = dao.upsertNote(NoteEntity(title = "Old", bodyMarkdown = "Remove"))
        dao.moveToTrash(id, deletedAt = 100L)

        val notes = dao.observeActiveNotes().first()

        assertTrue(notes.isEmpty())
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.listify.notes.data.db.NoteDaoTest
```

Expected: FAIL because `ListifyDatabase`, `NoteDao`, and `NoteEntity` do not exist.

- [ ] **Step 3: Add domain models**

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/domain/Models.kt`:

```kotlin
package com.listify.notes.domain

data class Note(
    val id: Long,
    val title: String,
    val bodyMarkdown: String,
    val preview: String,
    val folderId: Long?,
    val color: String?,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val isTrashed: Boolean,
    val isLocked: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)

data class Folder(val id: Long, val name: String, val sortOrder: Int, val color: String?)
data class Tag(val id: Long, val name: String, val color: String?)
data class Reminder(val id: Long, val noteId: Long, val scheduledAt: Long, val firedAt: Long?, val completedAt: Long?)
data class Attachment(val id: Long, val noteId: Long, val localPath: String, val mimeType: String, val displayName: String, val sizeBytes: Long)
```

- [ ] **Step 4: Add Room entities**

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/data/db/Entities.kt`:

```kotlin
package com.listify.notes.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "notes", indices = [Index("folderId"), Index("updatedAt")])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val bodyMarkdown: String,
    val preview: String = bodyMarkdown.take(160),
    val folderId: Long? = null,
    val color: String? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    val color: String? = null
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: String? = null
)

@Entity(primaryKeys = ["noteId", "tagId"], tableName = "note_tags")
data class NoteTagEntity(val noteId: Long, val tagId: Long)

@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("noteId"), Index("scheduledAt")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val scheduledAt: Long,
    val firedAt: Long? = null,
    val completedAt: Long? = null
)

@Entity(
    tableName = "attachments",
    foreignKeys = [ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("noteId")]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val localPath: String,
    val mimeType: String,
    val displayName: String,
    val sizeBytes: Long
)
```

- [ ] **Step 5: Add DAO and database**

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/data/db/NoteDao.kt`:

```kotlin
package com.listify.notes.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity): Long

    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeActiveNotes(): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET isTrashed = 1, deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :noteId")
    suspend fun moveToTrash(noteId: Long, deletedAt: Long)
}
```

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/data/db/ListifyDatabase.kt`:

```kotlin
package com.listify.notes.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteEntity::class,
        FolderEntity::class,
        TagEntity::class,
        NoteTagEntity::class,
        ReminderEntity::class,
        AttachmentEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ListifyDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
```

- [ ] **Step 6: Add repository boundary**

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/data/db/NoteRepository.kt`:

```kotlin
package com.listify.notes.data.db

import com.listify.notes.domain.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepository(private val dao: NoteDao) {
    fun observeActiveNotes(): Flow<List<Note>> = dao.observeActiveNotes().map { notes ->
        notes.map { it.toDomain() }
    }

    suspend fun createNote(title: String, bodyMarkdown: String): Long {
        return dao.upsertNote(NoteEntity(title = title, bodyMarkdown = bodyMarkdown))
    }

    suspend fun moveToTrash(noteId: Long, now: Long) {
        dao.moveToTrash(noteId, now)
    }
}

private fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    bodyMarkdown = bodyMarkdown,
    preview = preview,
    folderId = folderId,
    color = color,
    isPinned = isPinned,
    isArchived = isArchived,
    isTrashed = isTrashed,
    isLocked = isLocked,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)
```

- [ ] **Step 7: Run tests and commit**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.listify.notes.data.db.NoteDaoTest
```

Expected: PASS.

Commit:

```bash
git add app/src/main/java/com/listify/notes/domain app/src/main/java/com/listify/notes/data/db app/src/test/java/com/listify/notes/data/db
git commit -m "feat: add local notes database"
```

---

### Task 3: Search, Filters, And Locked Preview Rules

**Files:**
- Modify: `/root/ListifyApp/app/src/main/java/com/listify/notes/data/db/Entities.kt`
- Modify: `/root/ListifyApp/app/src/main/java/com/listify/notes/data/db/NoteDao.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/domain/SearchModels.kt`
- Create: `/root/ListifyApp/app/src/test/java/com/listify/notes/data/db/NoteSearchTest.kt`

- [ ] **Step 1: Write search tests**

Add `/root/ListifyApp/app/src/test/java/com/listify/notes/data/db/NoteSearchTest.kt`:

```kotlin
package com.listify.notes.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NoteSearchTest {
    private lateinit var db: ListifyDatabase
    private lateinit var dao: NoteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ListifyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.noteDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun searchReturnsNormalNotesByBodyText() = runTest {
        dao.upsertNote(NoteEntity(title = "Garden", bodyMarkdown = "Buy basil seeds"))

        val result = dao.searchNotes(query = "basil", includeArchived = false, includeTrashed = false)

        assertEquals("Garden", result.single().title)
    }

    @Test
    fun lockedNoteSearchHidesBodyPreview() = runTest {
        dao.upsertNote(NoteEntity(title = "Private", bodyMarkdown = "secret phrase", isLocked = true, preview = "secret phrase"))

        val result = dao.searchNotes(query = "Private", includeArchived = false, includeTrashed = false)

        assertEquals("Private", result.single().title)
        assertTrue(result.single().preview.isBlank())
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.listify.notes.data.db.NoteSearchTest
```

Expected: FAIL because `searchNotes` does not exist.

- [ ] **Step 3: Add search models**

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/domain/SearchModels.kt`:

```kotlin
package com.listify.notes.domain

data class SearchFilters(
    val query: String,
    val folderId: Long? = null,
    val tagId: Long? = null,
    val color: String? = null,
    val includeArchived: Boolean = false,
    val includeTrashed: Boolean = false,
    val onlyPinned: Boolean = false,
    val onlyWithReminder: Boolean = false,
    val updatedAfter: Long? = null,
    val updatedBefore: Long? = null
)

data class SearchResult(
    val noteId: Long,
    val title: String,
    val preview: String,
    val isLocked: Boolean,
    val updatedAt: Long
)
```

- [ ] **Step 4: Add DAO search query**

Update `/root/ListifyApp/app/src/main/java/com/listify/notes/data/db/NoteDao.kt`:

```kotlin
package com.listify.notes.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity): Long

    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeActiveNotes(): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET isTrashed = 1, deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :noteId")
    suspend fun moveToTrash(noteId: Long, deletedAt: Long)

    @Query(
        """
        SELECT id, title, CASE WHEN isLocked THEN '' ELSE preview END AS preview, isLocked, updatedAt
        FROM notes
        WHERE (:includeArchived OR isArchived = 0)
          AND (:includeTrashed OR isTrashed = 0)
          AND (
            title LIKE '%' || :query || '%'
            OR (isLocked = 0 AND bodyMarkdown LIKE '%' || :query || '%')
          )
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    suspend fun searchNotes(query: String, includeArchived: Boolean, includeTrashed: Boolean): List<NoteSearchRow>
}

data class NoteSearchRow(
    val id: Long,
    val title: String,
    val preview: String,
    val isLocked: Boolean,
    val updatedAt: Long
)
```

- [ ] **Step 5: Run tests and commit**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.listify.notes.data.db.NoteSearchTest
```

Expected: PASS.

Commit:

```bash
git add app/src/main/java/com/listify/notes/domain/SearchModels.kt app/src/main/java/com/listify/notes/data/db/NoteDao.kt app/src/test/java/com/listify/notes/data/db/NoteSearchTest.kt
git commit -m "feat: add local note search"
```

---

### Task 4: Hybrid Markdown Editor

**Files:**
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/domain/MarkdownFormatter.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/editor/MarkdownEditor.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/editor/MarkdownToolbar.kt`
- Create: `/root/ListifyApp/app/src/test/java/com/listify/notes/domain/MarkdownFormatterTest.kt`

- [ ] **Step 1: Write formatter tests**

Add `/root/ListifyApp/app/src/test/java/com/listify/notes/domain/MarkdownFormatterTest.kt`:

```kotlin
package com.listify.notes.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownFormatterTest {
    @Test
    fun boldWrapsSelectedText() {
        val result = MarkdownFormatter.apply(MarkdownAction.Bold, "hello", 0, 5)
        assertEquals("**hello**", result.text)
        assertEquals(9, result.selectionEnd)
    }

    @Test
    fun checklistPrefixesCurrentLine() {
        val result = MarkdownFormatter.apply(MarkdownAction.Checklist, "buy milk", 0, 0)
        assertEquals("- [ ] buy milk", result.text)
        assertEquals(6, result.selectionStart)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.listify.notes.domain.MarkdownFormatterTest
```

Expected: FAIL because `MarkdownFormatter` and `MarkdownAction` do not exist.

- [ ] **Step 3: Add formatter**

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/domain/MarkdownFormatter.kt`:

```kotlin
package com.listify.notes.domain

enum class MarkdownAction { Heading, Bold, Italic, Link, InlineCode, CodeBlock, Quote, Checklist, Bullet }

data class MarkdownEditResult(val text: String, val selectionStart: Int, val selectionEnd: Int)

object MarkdownFormatter {
    fun apply(action: MarkdownAction, text: String, selectionStart: Int, selectionEnd: Int): MarkdownEditResult {
        return when (action) {
            MarkdownAction.Bold -> wrap(text, selectionStart, selectionEnd, "**", "**")
            MarkdownAction.Italic -> wrap(text, selectionStart, selectionEnd, "*", "*")
            MarkdownAction.InlineCode -> wrap(text, selectionStart, selectionEnd, "`", "`")
            MarkdownAction.Link -> wrap(text, selectionStart, selectionEnd, "[", "](url)")
            MarkdownAction.CodeBlock -> wrap(text, selectionStart, selectionEnd, "```\n", "\n```")
            MarkdownAction.Heading -> prefixLine(text, selectionStart, "# ")
            MarkdownAction.Quote -> prefixLine(text, selectionStart, "> ")
            MarkdownAction.Checklist -> prefixLine(text, selectionStart, "- [ ] ")
            MarkdownAction.Bullet -> prefixLine(text, selectionStart, "- ")
        }
    }

    private fun wrap(text: String, start: Int, end: Int, before: String, after: String): MarkdownEditResult {
        val safeStart = start.coerceIn(0, text.length)
        val safeEnd = end.coerceIn(safeStart, text.length)
        val next = text.substring(0, safeStart) + before + text.substring(safeStart, safeEnd) + after + text.substring(safeEnd)
        return MarkdownEditResult(next, safeStart + before.length, safeEnd + before.length + after.length)
    }

    private fun prefixLine(text: String, cursor: Int, prefix: String): MarkdownEditResult {
        val safeCursor = cursor.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (safeCursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val next = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        val nextCursor = safeCursor + prefix.length
        return MarkdownEditResult(next, nextCursor, nextCursor)
    }
}
```

- [ ] **Step 4: Add editor UI components**

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/editor/MarkdownToolbar.kt`:

```kotlin
package com.listify.notes.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.listify.notes.domain.MarkdownAction

@Composable
fun MarkdownToolbar(onAction: (MarkdownAction) -> Unit) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        IconButton(onClick = { onAction(MarkdownAction.Heading) }) { Icon(Icons.Default.Title, contentDescription = "Heading") }
        IconButton(onClick = { onAction(MarkdownAction.Bold) }) { Icon(Icons.Default.FormatBold, contentDescription = "Bold") }
        IconButton(onClick = { onAction(MarkdownAction.Italic) }) { Icon(Icons.Default.FormatItalic, contentDescription = "Italic") }
        IconButton(onClick = { onAction(MarkdownAction.Link) }) { Icon(Icons.Default.Link, contentDescription = "Link") }
        IconButton(onClick = { onAction(MarkdownAction.InlineCode) }) { Icon(Icons.Default.Code, contentDescription = "Code") }
        IconButton(onClick = { onAction(MarkdownAction.Quote) }) { Icon(Icons.Default.FormatQuote, contentDescription = "Quote") }
        IconButton(onClick = { onAction(MarkdownAction.Checklist) }) { Icon(Icons.Default.CheckBox, contentDescription = "Checklist") }
        IconButton(onClick = { onAction(MarkdownAction.Bullet) }) { Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet list") }
    }
}
```

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/editor/MarkdownEditor.kt`:

```kotlin
package com.listify.notes.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.listify.notes.domain.MarkdownFormatter

@Composable
fun MarkdownEditor(
    title: String,
    body: String,
    onBodyChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var value by remember(body) { mutableStateOf(TextFieldValue(body)) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(text = title.ifBlank { "Untitled" })
        MarkdownToolbar { action ->
            val result = MarkdownFormatter.apply(action, value.text, value.selection.start, value.selection.end)
            value = TextFieldValue(result.text, TextRange(result.selectionStart, result.selectionEnd))
            onBodyChange(result.text)
        }
        BasicTextField(
            value = value,
            onValueChange = {
                value = it
                onBodyChange(it.text)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

- [ ] **Step 5: Run tests and commit**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.listify.notes.domain.MarkdownFormatterTest
```

Expected: PASS.

Commit:

```bash
git add app/src/main/java/com/listify/notes/domain/MarkdownFormatter.kt app/src/main/java/com/listify/notes/ui/editor app/src/test/java/com/listify/notes/domain/MarkdownFormatterTest.kt
git commit -m "feat: add hybrid markdown editor"
```

---

### Task 5: Backup Export And Import Core

**Files:**
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/data/backup/BackupModels.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/data/backup/BackupCodec.kt`
- Create: `/root/ListifyApp/app/src/test/java/com/listify/notes/data/backup/BackupCodecTest.kt`

- [ ] **Step 1: Write backup round-trip test**

Add `/root/ListifyApp/app/src/test/java/com/listify/notes/data/backup/BackupCodecTest.kt`:

```kotlin
package com.listify.notes.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCodecTest {
    @Test
    fun backupJsonRoundTripsNotesAndFolders() {
        val archive = BackupArchive(
            schemaVersion = 1,
            exportedAt = 200L,
            notes = listOf(BackupNote(id = 1, title = "A", bodyMarkdown = "Body", updatedAt = 100L)),
            folders = listOf(BackupFolder(id = 2, name = "Work", sortOrder = 0))
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(archive))

        assertEquals(archive, decoded)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.listify.notes.data.backup.BackupCodecTest
```

Expected: FAIL because backup types do not exist.

- [ ] **Step 3: Add backup models and codec**

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/data/backup/BackupModels.kt`:

```kotlin
package com.listify.notes.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupArchive(
    val schemaVersion: Int,
    val exportedAt: Long,
    val notes: List<BackupNote>,
    val folders: List<BackupFolder> = emptyList(),
    val tags: List<BackupTag> = emptyList(),
    val reminders: List<BackupReminder> = emptyList(),
    val attachments: List<BackupAttachment> = emptyList()
)

@Serializable
data class BackupNote(
    val id: Long,
    val title: String,
    val bodyMarkdown: String,
    val updatedAt: Long,
    val folderId: Long? = null,
    val color: String? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isLocked: Boolean = false
)

@Serializable
data class BackupFolder(val id: Long, val name: String, val sortOrder: Int, val color: String? = null)
@Serializable
data class BackupTag(val id: Long, val name: String, val color: String? = null)
@Serializable
data class BackupReminder(val id: Long, val noteId: Long, val scheduledAt: Long, val completedAt: Long? = null)
@Serializable
data class BackupAttachment(val id: Long, val noteId: Long, val archivePath: String, val mimeType: String, val displayName: String, val sizeBytes: Long)
```

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/data/backup/BackupCodec.kt`:

```kotlin
package com.listify.notes.data.backup

import kotlinx.serialization.json.Json

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    fun encode(archive: BackupArchive): String = json.encodeToString(BackupArchive.serializer(), archive)

    fun decode(raw: String): BackupArchive {
        val archive = json.decodeFromString(BackupArchive.serializer(), raw)
        require(archive.schemaVersion == 1) { "Unsupported backup schema version: ${archive.schemaVersion}" }
        return archive
    }
}
```

- [ ] **Step 4: Run tests and commit**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.listify.notes.data.backup.BackupCodecTest
```

Expected: PASS.

Commit:

```bash
git add app/src/main/java/com/listify/notes/data/backup app/src/test/java/com/listify/notes/data/backup
git commit -m "feat: add backup archive codec"
```

---

### Task 6: Privacy And Locked Notes

**Files:**
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/data/security/LockedNoteCipher.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/data/security/AppLockState.kt`
- Create: `/root/ListifyApp/app/src/test/java/com/listify/notes/data/security/LockedNoteCipherTest.kt`

- [ ] **Step 1: Write cipher behavior test**

Add `/root/ListifyApp/app/src/test/java/com/listify/notes/data/security/LockedNoteCipherTest.kt`:

```kotlin
package com.listify.notes.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LockedNoteCipherTest {
    @Test
    fun encryptedTextDecryptsToOriginalText() {
        val cipher = LockedNoteCipher(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16))

        val encrypted = cipher.encrypt("private note")
        val decrypted = cipher.decrypt(encrypted)

        assertNotEquals("private note", encrypted)
        assertEquals("private note", decrypted)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.listify.notes.data.security.LockedNoteCipherTest
```

Expected: FAIL because `LockedNoteCipher` does not exist.

- [ ] **Step 3: Add cipher and app lock state**

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/data/security/LockedNoteCipher.kt`:

```kotlin
package com.listify.notes.data.security

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class LockedNoteCipher(private val rawKey: ByteArray) {
    fun encrypt(plainText: String): String {
        val iv = Random.Default.nextBytes(12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(rawKey, "AES"), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(rawKey, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }
}
```

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/data/security/AppLockState.kt`:

```kotlin
package com.listify.notes.data.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppLockState {
    private val unlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = unlocked

    fun markUnlocked() {
        unlocked.value = true
    }

    fun lock() {
        unlocked.value = false
    }
}
```

- [ ] **Step 4: Run tests and commit**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.listify.notes.data.security.LockedNoteCipherTest
```

Expected: PASS.

Commit:

```bash
git add app/src/main/java/com/listify/notes/data/security app/src/test/java/com/listify/notes/data/security
git commit -m "feat: add locked note security core"
```

---

### Task 7: Reminders And Attachments

**Files:**
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/data/reminders/ReminderScheduler.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/data/files/AttachmentStore.kt`
- Create: `/root/ListifyApp/app/src/test/java/com/listify/notes/data/reminders/ReminderSchedulerTest.kt`

- [ ] **Step 1: Write reminder scheduling test**

Add `/root/ListifyApp/app/src/test/java/com/listify/notes/data/reminders/ReminderSchedulerTest.kt`:

```kotlin
package com.listify.notes.data.reminders

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSchedulerTest {
    @Test
    fun buildsStableWorkNameForNoteReminder() {
        assertEquals("note-reminder-42", ReminderScheduler.workName(noteId = 42))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.listify.notes.data.reminders.ReminderSchedulerTest
```

Expected: FAIL because `ReminderScheduler` does not exist.

- [ ] **Step 3: Add reminder scheduler boundary**

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/data/reminders/ReminderScheduler.kt`:

```kotlin
package com.listify.notes.data.reminders

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    fun schedule(noteId: Long, scheduledAtMillis: Long, nowMillis: Long = System.currentTimeMillis()) {
        val delay = (scheduledAtMillis - nowMillis).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putLong("noteId", noteId).build())
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    companion object {
        fun workName(noteId: Long): String = "note-reminder-$noteId"
    }
}
```

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/data/reminders/ReminderWorker.kt`:

```kotlin
package com.listify.notes.data.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return Result.success()
    }
}
```

- [ ] **Step 4: Add attachment storage boundary**

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/data/files/AttachmentStore.kt`:

```kotlin
package com.listify.notes.data.files

import android.content.Context
import android.net.Uri
import java.io.File

class AttachmentStore(private val context: Context) {
    fun attachmentFile(noteId: Long, displayName: String): File {
        val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val dir = File(context.filesDir, "attachments/$noteId").apply { mkdirs() }
        return File(dir, safeName)
    }

    fun copyIntoNote(noteId: Long, source: Uri, displayName: String): File {
        val target = attachmentFile(noteId, displayName)
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "Could not open attachment source" }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return target
    }
}
```

- [ ] **Step 5: Run tests and commit**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.listify.notes.data.reminders.ReminderSchedulerTest
```

Expected: PASS.

Commit:

```bash
git add app/src/main/java/com/listify/notes/data/reminders app/src/main/java/com/listify/notes/data/files app/src/test/java/com/listify/notes/data/reminders
git commit -m "feat: add reminders and attachment storage"
```

---

### Task 8: Compose Navigation And Main Screens

**Files:**
- Modify: `/root/ListifyApp/app/src/main/java/com/listify/notes/ListifyRoot.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/navigation/ListifyDestination.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/screens/NotesScreen.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/screens/FoldersScreen.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/screens/SearchScreen.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/screens/RemindersScreen.kt`
- Create: `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/screens/SettingsScreen.kt`
- Create: `/root/ListifyApp/app/src/androidTest/java/com/listify/notes/ListifyNavigationTest.kt`

- [ ] **Step 1: Write navigation UI test**

Add `/root/ListifyApp/app/src/androidTest/java/com/listify/notes/ListifyNavigationTest.kt`:

```kotlin
package com.listify.notes

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ListifyNavigationTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationOpensSearch() {
        rule.onNodeWithText("Search").performClick()
        rule.onNodeWithText("Find anything").assertExists()
    }
}
```

- [ ] **Step 2: Run UI test to verify it fails**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.listify.notes.ListifyNavigationTest
```

Expected: FAIL until bottom navigation and the Search screen exist. If no emulator is connected, run this after starting an Android emulator.

- [ ] **Step 3: Add destinations and screens**

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/navigation/ListifyDestination.kt`:

```kotlin
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
```

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/screens/NotesScreen.kt`:

```kotlin
package com.listify.notes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NotesScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("All notes")
        Text("Create your first offline note.")
    }
}
```

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/screens/FoldersScreen.kt`:

```kotlin
package com.listify.notes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FoldersScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("Folders")
        Text("Organize notes into notebooks.")
    }
}
```

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/screens/SearchScreen.kt`:

```kotlin
package com.listify.notes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SearchScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("Find anything")
        Text("Search titles, note bodies, tags, and folders.")
    }
}
```

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/screens/RemindersScreen.kt`:

```kotlin
package com.listify.notes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RemindersScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("Reminders")
        Text("Upcoming and overdue note reminders.")
    }
}
```

Add `/root/ListifyApp/app/src/main/java/com/listify/notes/ui/screens/SettingsScreen.kt`:

```kotlin
package com.listify.notes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    Column(Modifier.padding(16.dp)) {
        Text("Settings")
        Text("Privacy, backups, theme, and trash.")
    }
}
```

- [ ] **Step 4: Replace root with bottom navigation**

Update `/root/ListifyApp/app/src/main/java/com/listify/notes/ListifyRoot.kt`:

```kotlin
package com.listify.notes

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.listify.notes.ui.navigation.ListifyDestination
import com.listify.notes.ui.screens.FoldersScreen
import com.listify.notes.ui.screens.NotesScreen
import com.listify.notes.ui.screens.RemindersScreen
import com.listify.notes.ui.screens.SearchScreen
import com.listify.notes.ui.screens.SettingsScreen

@Composable
fun ListifyRoot() {
    var selected by remember { mutableStateOf(ListifyDestination.Notes) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                ListifyDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = selected == destination,
                        onClick = { selected = destination },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
            when (selected) {
                ListifyDestination.Notes -> NotesScreen()
                ListifyDestination.Folders -> FoldersScreen()
                ListifyDestination.Search -> SearchScreen()
                ListifyDestination.Reminders -> RemindersScreen()
                ListifyDestination.Settings -> SettingsScreen()
            }
        }
    }
}
```

- [ ] **Step 5: Run build and UI test**

Run:

```bash
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.listify.notes.ListifyNavigationTest
```

Expected: assemble passes. UI test passes when an emulator or device is connected.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/listify/notes/ListifyRoot.kt app/src/main/java/com/listify/notes/ui app/src/androidTest/java/com/listify/notes/ListifyNavigationTest.kt
git commit -m "feat: add main notes navigation"
```

---

### Task 9: Final Verification And Documentation

**Files:**
- Modify: `/root/ListifyApp/docs/superpowers/specs/2026-05-18-offline-notes-app-design.md`
- Create: `/root/ListifyApp/README.md`

- [ ] **Step 1: Add README**

Add `/root/ListifyApp/README.md`:

```markdown
# Listify

Listify is an offline-first Android notes app built with Kotlin, Jetpack Compose, Material 3, and Room.

## First Release Scope

- Hybrid Markdown notes.
- Folders, tags, colors, pinned notes, archive, and trash.
- Local search and filters.
- Local reminders.
- Local attachments.
- App lock and locked notes.
- Local backup export/import.

## Build

```bash
./gradlew :app:assembleDebug
```

## Test

```bash
./gradlew :app:testDebugUnitTest
```

Run instrumentation tests with an emulator or Android device:

```bash
./gradlew :app:connectedDebugAndroidTest
```
```

- [ ] **Step 2: Run full verification**

Run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Expected: both commands finish with `BUILD SUCCESSFUL`.

Run with a connected emulator:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected: `BUILD SUCCESSFUL`. If there is no connected emulator or device, record that instrumentation verification was not run.

- [ ] **Step 3: Check offline-only constraint**

Run:

```bash
rg -n "android.permission.INTERNET|Firebase|auth|login|sign in|sync|cloud" /root/ListifyApp/app /root/ListifyApp/README.md
```

Expected: no app code requiring network, cloud, accounts, auth, login, or sync.

- [ ] **Step 4: Commit**

```bash
git add README.md docs/superpowers/specs/2026-05-18-offline-notes-app-design.md
git commit -m "docs: add notes app build guide"
```

---

## Coverage Check

- Android scaffold: Task 1.
- Room storage and data model: Task 2.
- Search and locked preview rules: Task 3.
- Hybrid Markdown editor: Task 4.
- Backup archive core: Task 5.
- App lock and locked-note encryption boundary: Task 6.
- Local reminders and attachments: Task 7.
- Warm Material 3 bottom navigation and main screens: Task 8.
- Verification and offline-only documentation: Task 9.

This plan intentionally builds the product in layers so each task can be tested and committed independently.
