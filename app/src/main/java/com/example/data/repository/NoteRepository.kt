package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.model.BackupContainer
import com.example.data.model.DrawingStroke
import com.example.data.model.InkColors
import com.example.data.model.NoteBackupItem
import com.example.data.model.NoteEntity
import com.example.data.model.PaperPattern
import com.example.data.model.PaperTheme
import com.example.data.model.PointData
import com.example.data.model.TextBlock
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NoteRepository(private val noteDao: NoteDao) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val strokeListType = Types.newParameterizedType(List::class.java, DrawingStroke::class.java)
    private val strokeListAdapter = moshi.adapter<List<DrawingStroke>>(strokeListType)

    private val textBlockListType = Types.newParameterizedType(List::class.java, TextBlock::class.java)
    private val textBlockListAdapter = moshi.adapter<List<TextBlock>>(textBlockListType)

    private val backupAdapter = moshi.adapter(BackupContainer::class.java)

    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun getFavoriteNotes(): Flow<List<NoteEntity>> = noteDao.getFavoriteNotes()

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    suspend fun getNote(id: Long): NoteEntity? = withContext(Dispatchers.IO) {
        noteDao.getNoteById(id)
    }

    suspend fun saveNote(
        id: Long = 0L,
        title: String,
        strokes: List<DrawingStroke>,
        textBlocks: List<TextBlock>,
        paperTheme: PaperTheme,
        paperPattern: PaperPattern,
        isFavorite: Boolean = false
    ): Long = withContext(Dispatchers.IO) {
        val strokesJson = strokeListAdapter.toJson(strokes)
        val textBlocksJson = textBlockListAdapter.toJson(textBlocks)
        val previewSnippet = textBlocks.firstOrNull()?.text?.take(80)
            ?: if (strokes.isNotEmpty()) "${strokes.size} pen stroke(s)" else "Empty note"

        val entity = NoteEntity(
            id = id,
            title = title.ifBlank { "Untitled Note" },
            previewSnippet = previewSnippet,
            strokesJson = strokesJson,
            textBlocksJson = textBlocksJson,
            paperThemeName = paperTheme.name,
            paperPatternName = paperPattern.name,
            isFavorite = isFavorite,
            updatedAt = System.currentTimeMillis()
        )

        if (id == 0L) {
            noteDao.insertNote(entity)
        } else {
            noteDao.updateNote(entity)
            id
        }
    }

    suspend fun toggleFavorite(note: NoteEntity) = withContext(Dispatchers.IO) {
        noteDao.updateNote(note.copy(isFavorite = !note.isFavorite))
    }

    suspend fun deleteNote(id: Long) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteById(id)
    }

    fun parseStrokes(json: String): List<DrawingStroke> {
        return try {
            if (json.isBlank()) emptyList()
            else strokeListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseTextBlocks(json: String): List<TextBlock> {
        return try {
            if (json.isBlank()) emptyList()
            else textBlockListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val notes = noteDao.getAllNotesSnapshot()
        val backupItems = notes.map {
            NoteBackupItem(
                id = it.id,
                title = it.title,
                previewSnippet = it.previewSnippet,
                strokesJson = it.strokesJson,
                textBlocksJson = it.textBlocksJson,
                paperThemeName = it.paperThemeName,
                paperPatternName = it.paperPatternName,
                isFavorite = it.isFavorite,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
        val container = BackupContainer(
            notes = backupItems
        )
        backupAdapter.indent("  ").toJson(container)
    }

    suspend fun importBackupJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val container = backupAdapter.fromJson(jsonString)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid backup format"))

            val entities = container.notes.map {
                NoteEntity(
                    id = 0L, // fresh IDs on import to avoid conflict
                    title = it.title,
                    previewSnippet = it.previewSnippet,
                    strokesJson = it.strokesJson,
                    textBlocksJson = it.textBlocksJson,
                    paperThemeName = it.paperThemeName,
                    paperPatternName = it.paperPatternName,
                    isFavorite = it.isFavorite,
                    createdAt = it.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            }
            noteDao.insertAll(entities)
            Result.success(entities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun seedInitialWelcomeNoteIfEmpty() = withContext(Dispatchers.IO) {
        val count = noteDao.getAllNotesSnapshot().size
        if (count == 0) {
            val introTextBlocks = listOf(
                TextBlock(
                    text = "Welcome to InkNote ✨",
                    x = 80f,
                    y = 120f,
                    fontSizeSp = 24f,
                    colorHex = InkColors.BLACK_HEX,
                    isBold = true
                ),
                TextBlock(
                    text = "Crafted for Vivo V23 5G with Stylus support.\n" +
                            "• 3 Minimalist Tools: Pen, Eraser, and Text.\n" +
                            "• 4 Essential Inks: Black, Blue, Red, White.\n" +
                            "• Palm Rejection & Pressure-sensitive strokes.\n" +
                            "• PDF Export & Cloud Sync.\n\n" +
                            "Try sketching below!",
                    x = 80f,
                    y = 180f,
                    fontSizeSp = 16f,
                    colorHex = InkColors.BLACK_HEX,
                    isBold = false
                )
            )

            // A decorative initial stroke
            val sampleStrokes = listOf(
                DrawingStroke(
                    points = listOf(
                        PointData(100f, 440f, 0.8f),
                        PointData(180f, 420f, 1.0f),
                        PointData(280f, 450f, 1.2f),
                        PointData(400f, 410f, 0.9f),
                        PointData(520f, 430f, 0.7f)
                    ),
                    colorHex = InkColors.BLUE_HEX,
                    strokeWidth = 6f
                ),
                DrawingStroke(
                    points = listOf(
                        PointData(120f, 480f, 0.8f),
                        PointData(220f, 460f, 1.0f),
                        PointData(360f, 490f, 1.1f),
                        PointData(480f, 470f, 0.8f)
                    ),
                    colorHex = InkColors.RED_HEX,
                    strokeWidth = 4f
                )
            )

            saveNote(
                id = 0L,
                title = "Welcome to InkNote",
                strokes = sampleStrokes,
                textBlocks = introTextBlocks,
                paperTheme = PaperTheme.LIGHT_RULED,
                paperPattern = PaperPattern.RULED,
                isFavorite = true
            )
        }
    }
}
