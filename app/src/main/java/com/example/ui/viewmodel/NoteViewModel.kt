package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ActiveTool
import com.example.data.model.DrawingStroke
import com.example.data.model.InkColors
import com.example.data.model.NoteEntity
import com.example.data.model.PaperPattern
import com.example.data.model.PaperTheme
import com.example.data.model.TextBlock
import com.example.data.repository.NoteRepository
import com.example.drawing.DrawingEngine
import com.example.drawing.ImageExporter
import com.example.drawing.PdfExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream

data class CanvasSnapshot(
    val strokes: List<DrawingStroke>,
    val textBlocks: List<TextBlock>
)

sealed interface PdfExportState {
    data object Idle : PdfExportState
    data object Generating : PdfExportState
    data class Ready(val file: File, val shareUri: Uri, val title: String) : PdfExportState
    data class Error(val message: String) : PdfExportState
}

sealed interface PngExportState {
    data object Idle : PngExportState
    data object Generating : PngExportState
    data class Ready(val file: File, val shareUri: Uri, val title: String) : PngExportState
    data class Error(val message: String) : PngExportState
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    init {
        val db = AppDatabase.getInstance(application)
        repository = NoteRepository(db.noteDao())
        viewModelScope.launch {
            repository.seedInitialWelcomeNoteIfEmpty()
        }
    }

    val notes: StateFlow<List<NoteEntity>> = repository.getAllNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode = _themeMode.asStateFlow()

    // Current Note Editor State
    private val _currentNoteId = MutableStateFlow<Long?>(null)
    val currentNoteId = _currentNoteId.asStateFlow()

    private val _noteTitle = MutableStateFlow("Untitled Note")
    val noteTitle = _noteTitle.asStateFlow()

    private val _strokes = MutableStateFlow<List<DrawingStroke>>(emptyList())
    val strokes = _strokes.asStateFlow()

    private val _textBlocks = MutableStateFlow<List<TextBlock>>(emptyList())
    val textBlocks = _textBlocks.asStateFlow()

    private val _activeTool = MutableStateFlow(ActiveTool.PEN)
    val activeTool = _activeTool.asStateFlow()

    private val _activeInkColor = MutableStateFlow(InkColors.BLACK_HEX)
    val activeInkColor = _activeInkColor.asStateFlow()

    private val _penWidth = MutableStateFlow(5f)
    val penWidth = _penWidth.asStateFlow()

    private val _eraserRadius = MutableStateFlow(28f)
    val eraserRadius = _eraserRadius.asStateFlow()

    private val _paperTheme = MutableStateFlow(PaperTheme.LIGHT_RULED)
    val paperTheme = _paperTheme.asStateFlow()

    private val _paperPattern = MutableStateFlow(PaperPattern.RULED)
    val paperPattern = _paperPattern.asStateFlow()

    // Stylus & Palm Rejection for Vivo V23 5G
    private val _stylusOnlyMode = MutableStateFlow(false)
    val stylusOnlyMode = _stylusOnlyMode.asStateFlow()

    private val _isStylusDetected = MutableStateFlow(false)
    val isStylusDetected = _isStylusDetected.asStateFlow()

    // Undo / Redo Stacks
    private val undoStack = mutableListOf<CanvasSnapshot>()
    private val redoStack = mutableListOf<CanvasSnapshot>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    // Status & Dialogs
    private val _pdfExportState = MutableStateFlow<PdfExportState>(PdfExportState.Idle)
    val pdfExportState = _pdfExportState.asStateFlow()

    private val _pngExportState = MutableStateFlow<PngExportState>(PngExportState.Idle)
    val pngExportState = _pngExportState.asStateFlow()

    private val _cloudSyncMessage = MutableStateFlow<String?>(null)
    val cloudSyncMessage = _cloudSyncMessage.asStateFlow()

    private var autoSaveJob: Job? = null

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun toggleStylusOnlyMode() {
        _stylusOnlyMode.value = !_stylusOnlyMode.value
    }

    fun onStylusInputDetected() {
        _isStylusDetected.value = true
    }

    fun selectTool(tool: ActiveTool) {
        _activeTool.value = tool
    }

    fun selectInkColor(colorHex: Long) {
        _activeInkColor.value = colorHex
    }

    fun setPenWidth(width: Float) {
        _penWidth.value = width
    }

    fun setEraserRadius(radius: Float) {
        _eraserRadius.value = radius
    }

    fun setPaperTheme(theme: PaperTheme) {
        _paperTheme.value = theme
        // If switching to dark paper and ink was black, gently suggest white ink
        if (theme.isDark && _activeInkColor.value == InkColors.BLACK_HEX) {
            _activeInkColor.value = InkColors.WHITE_HEX
        } else if (!theme.isDark && _activeInkColor.value == InkColors.WHITE_HEX) {
            _activeInkColor.value = InkColors.BLACK_HEX
        }
        triggerAutoSave()
    }

    fun setPaperPattern(pattern: PaperPattern) {
        _paperPattern.value = pattern
        triggerAutoSave()
    }

    fun setNoteTitle(title: String) {
        _noteTitle.value = title
        triggerAutoSave()
    }

    fun openNote(id: Long) {
        viewModelScope.launch {
            val note = repository.getNote(id)
            if (note != null) {
                _currentNoteId.value = note.id
                _noteTitle.value = note.title
                _strokes.value = repository.parseStrokes(note.strokesJson)
                _textBlocks.value = repository.parseTextBlocks(note.textBlocksJson)
                _paperTheme.value = try {
                    PaperTheme.valueOf(note.paperThemeName)
                } catch (e: Exception) {
                    PaperTheme.LIGHT_RULED
                }
                _paperPattern.value = try {
                    PaperPattern.valueOf(note.paperPatternName)
                } catch (e: Exception) {
                    PaperPattern.RULED
                }
                undoStack.clear()
                redoStack.clear()
                updateUndoRedoStates()
            }
        }
    }

    fun closeCurrentNote() {
        saveCurrentNote()
        _currentNoteId.value = null
    }

    fun createNewNote(
        title: String = "Untitled Note",
        initialPaperTheme: PaperTheme? = null
    ) {
        val theme = initialPaperTheme ?: if (_themeMode.value == ThemeMode.DARK) PaperTheme.DARK_SLATE else PaperTheme.LIGHT_RULED
        _currentNoteId.value = 0L
        _noteTitle.value = title
        _strokes.value = emptyList()
        _textBlocks.value = emptyList()
        _paperTheme.value = theme
        _paperPattern.value = PaperPattern.RULED
        _activeTool.value = ActiveTool.PEN
        _activeInkColor.value = if (theme.isDark) InkColors.WHITE_HEX else InkColors.BLACK_HEX
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoStates()
    }

    private fun pushSnapshot() {
        undoStack.add(CanvasSnapshot(_strokes.value, _textBlocks.value))
        if (undoStack.size > 40) undoStack.removeAt(0)
        redoStack.clear()
        updateUndoRedoStates()
    }

    private fun updateUndoRedoStates() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val current = CanvasSnapshot(_strokes.value, _textBlocks.value)
            redoStack.add(current)
            val previous = undoStack.removeAt(undoStack.lastIndex)
            _strokes.value = previous.strokes
            _textBlocks.value = previous.textBlocks
            updateUndoRedoStates()
            triggerAutoSave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val current = CanvasSnapshot(_strokes.value, _textBlocks.value)
            undoStack.add(current)
            val next = redoStack.removeAt(redoStack.lastIndex)
            _strokes.value = next.strokes
            _textBlocks.value = next.textBlocks
            updateUndoRedoStates()
            triggerAutoSave()
        }
    }

    fun addStroke(stroke: DrawingStroke) {
        pushSnapshot()
        _strokes.value = _strokes.value + stroke
        triggerAutoSave()
    }

    fun eraseStrokesAt(offset: androidx.compose.ui.geometry.Offset, radius: Float) {
        val currentStrokes = _strokes.value
        val remaining = currentStrokes.filterNot { stroke ->
            DrawingEngine.isStrokeIntersectingEraser(stroke, offset, radius)
        }
        if (remaining.size != currentStrokes.size) {
            pushSnapshot()
            _strokes.value = remaining
            triggerAutoSave()
        }
    }

    fun clearCanvas() {
        if (_strokes.value.isNotEmpty() || _textBlocks.value.isNotEmpty()) {
            pushSnapshot()
            _strokes.value = emptyList()
            _textBlocks.value = emptyList()
            triggerAutoSave()
        }
    }

    fun addTextBlock(text: String, x: Float, y: Float, fontSizeSp: Float = 18f) {
        if (text.isBlank()) return
        pushSnapshot()
        val block = TextBlock(
            text = text,
            x = x,
            y = y,
            fontSizeSp = fontSizeSp,
            colorHex = _activeInkColor.value
        )
        _textBlocks.value = _textBlocks.value + block
        triggerAutoSave()
    }

    fun updateTextBlock(id: String, newText: String, x: Float, y: Float) {
        pushSnapshot()
        _textBlocks.value = _textBlocks.value.map {
            if (it.id == id) it.copy(text = newText, x = x, y = y) else it
        }
        triggerAutoSave()
    }

    fun deleteTextBlock(id: String) {
        pushSnapshot()
        _textBlocks.value = _textBlocks.value.filterNot { it.id == id }
        triggerAutoSave()
    }

    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            saveCurrentNote()
        }
    }

    fun saveCurrentNote() {
        val currentId = _currentNoteId.value ?: return
        viewModelScope.launch {
            val savedId = repository.saveNote(
                id = currentId,
                title = _noteTitle.value,
                strokes = _strokes.value,
                textBlocks = _textBlocks.value,
                paperTheme = _paperTheme.value,
                paperPattern = _paperPattern.value
            )
            _currentNoteId.value = savedId
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
            if (_currentNoteId.value == id) {
                _currentNoteId.value = null
            }
        }
    }

    fun toggleFavorite(note: NoteEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(note)
        }
    }

    fun exportToPdf(width: Float, height: Float) {
        _pdfExportState.value = PdfExportState.Generating
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val file = PdfExporter.generatePdf(
                    context = context,
                    title = _noteTitle.value,
                    strokes = _strokes.value,
                    textBlocks = _textBlocks.value,
                    paperTheme = _paperTheme.value,
                    paperPattern = _paperPattern.value,
                    canvasWidth = width,
                    canvasHeight = height
                )
                val uri = PdfExporter.getFileUri(context, file)
                _pdfExportState.value = PdfExportState.Ready(file, uri, _noteTitle.value)
            } catch (e: Exception) {
                _pdfExportState.value = PdfExportState.Error(e.localizedMessage ?: "Failed to export PDF")
            }
        }
    }

    fun exportPdfToStream(outputStream: OutputStream, width: Float, height: Float, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                PdfExporter.generatePdf(
                    context = context,
                    title = _noteTitle.value,
                    strokes = _strokes.value,
                    textBlocks = _textBlocks.value,
                    paperTheme = _paperTheme.value,
                    paperPattern = _paperPattern.value,
                    canvasWidth = width,
                    canvasHeight = height,
                    outputStream = outputStream
                )
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun dismissPdfExportDialog() {
        _pdfExportState.value = PdfExportState.Idle
    }

    fun exportToPng(width: Float, height: Float) {
        _pngExportState.value = PngExportState.Generating
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val file = ImageExporter.generatePng(
                    context = context,
                    title = _noteTitle.value,
                    strokes = _strokes.value,
                    textBlocks = _textBlocks.value,
                    paperTheme = _paperTheme.value,
                    paperPattern = _paperPattern.value,
                    canvasWidth = width,
                    canvasHeight = height
                )
                val uri = ImageExporter.getFileUri(context, file)
                _pngExportState.value = PngExportState.Ready(file, uri, _noteTitle.value)
            } catch (e: Exception) {
                _pngExportState.value = PngExportState.Error(e.localizedMessage ?: "Failed to export PNG image")
            }
        }
    }

    fun exportPngToStream(outputStream: OutputStream, width: Float, height: Float, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                ImageExporter.generatePng(
                    context = context,
                    title = _noteTitle.value,
                    strokes = _strokes.value,
                    textBlocks = _textBlocks.value,
                    paperTheme = _paperTheme.value,
                    paperPattern = _paperPattern.value,
                    canvasWidth = width,
                    canvasHeight = height,
                    outputStream = outputStream
                )
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun dismissPngExportDialog() {
        _pngExportState.value = PngExportState.Idle
    }

    fun exportAllNotesBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = repository.exportBackupJson()
                _cloudSyncMessage.value = "Backup exported successfully!"
                onResult(json)
            } catch (e: Exception) {
                _cloudSyncMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun importNotesBackup(json: String, onComplete: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            val result = repository.importBackupJson(json)
            result.onSuccess { count ->
                _cloudSyncMessage.value = "Successfully imported $count note(s)!"
                onComplete(true, count)
            }.onFailure {
                _cloudSyncMessage.value = "Import failed: ${it.localizedMessage}"
                onComplete(false, 0)
            }
        }
    }

    fun clearCloudSyncMessage() {
        _cloudSyncMessage.value = null
    }
}
