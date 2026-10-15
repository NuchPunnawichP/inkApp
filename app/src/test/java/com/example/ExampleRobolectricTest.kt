package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.DrawingStroke
import com.example.data.model.InkColors
import com.example.data.model.PaperPattern
import com.example.data.model.PaperTheme
import com.example.data.model.PointData
import com.example.data.repository.NoteRepository
import com.example.drawing.DrawingEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: NoteRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NoteRepository(database.noteDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read app_name from context matches InkNote`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("InkNote", appName)
    }

    @Test
    fun `drawing engine pressure width modulation`() {
        val baseWidth = 5.0f
        val lightWidth = DrawingEngine.calculateEffectiveWidth(baseWidth, 0.2f)
        val heavyWidth = DrawingEngine.calculateEffectiveWidth(baseWidth, 1.2f)

        assertTrue(lightWidth < heavyWidth)
        assertTrue(heavyWidth > baseWidth)
    }

    @Test
    fun `drawing engine eraser detects stroke intersection`() {
        val stroke = DrawingStroke(
            points = listOf(
                PointData(10f, 10f),
                PointData(50f, 50f),
                PointData(100f, 100f)
            ),
            strokeWidth = 4f
        )

        // Close to the segment (50, 50)
        val hits = DrawingEngine.isStrokeIntersectingEraser(stroke, Offset(52f, 51f), eraserRadius = 15f)
        assertTrue(hits)

        // Far away from stroke
        val misses = DrawingEngine.isStrokeIntersectingEraser(stroke, Offset(300f, 300f), eraserRadius = 15f)
        assertFalse(misses)
    }

    @Test
    fun `repository saves note and exports-imports backup json`() = runBlocking {
        val strokes = listOf(
            DrawingStroke(
                points = listOf(PointData(10f, 20f), PointData(30f, 40f)),
                colorHex = InkColors.BLUE_HEX,
                strokeWidth = 5f
            )
        )

        val id = repository.saveNote(
            id = 0L,
            title = "Test Stylus Sketch",
            strokes = strokes,
            textBlocks = emptyList(),
            paperTheme = PaperTheme.DARK_SLATE,
            paperPattern = PaperPattern.GRID
        )
        assertTrue(id > 0)

        val savedNotes = repository.getAllNotes().first()
        assertEquals(1, savedNotes.size)
        assertEquals("Test Stylus Sketch", savedNotes[0].title)

        // Test Backup Export
        val backupJson = repository.exportBackupJson()
        assertTrue(backupJson.contains("Test Stylus Sketch"))
        assertTrue(backupJson.contains("InkNote"))

        // Test Backup Import
        val importResult = repository.importBackupJson(backupJson)
        assertTrue(importResult.isSuccess)
        assertEquals(1, importResult.getOrNull())

        val allAfterImport = repository.getAllNotes().first()
        assertEquals(2, allAfterImport.size)
    }

    @Test
    fun `imageExporter generates valid PNG file`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val strokes = listOf(
            DrawingStroke(
                points = listOf(PointData(50f, 50f), PointData(150f, 150f)),
                colorHex = InkColors.RED_HEX,
                strokeWidth = 6f
            )
        )

        val file = com.example.drawing.ImageExporter.generatePng(
            context = context,
            title = "TestPngExport",
            strokes = strokes,
            textBlocks = emptyList(),
            paperTheme = PaperTheme.LIGHT_CLEAN,
            paperPattern = PaperPattern.BLANK,
            canvasWidth = 800f,
            canvasHeight = 1200f
        )

        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertTrue(file.name.endsWith(".png"))
    }
}
