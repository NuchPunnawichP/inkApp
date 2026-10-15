package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.drawing.ImageExporter
import com.example.ui.viewmodel.PngExportState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ActiveTool
import com.example.data.model.DrawingStroke
import com.example.data.model.InkColors
import com.example.data.model.PaperPattern
import com.example.data.model.PaperTheme
import com.example.data.model.PointData
import com.example.data.model.TextBlock
import com.example.drawing.DrawingEngine
import com.example.drawing.PdfExporter
import com.example.ui.viewmodel.NoteViewModel
import com.example.ui.viewmodel.PdfExportState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvasScreen(
    viewModel: NoteViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val noteTitle by viewModel.noteTitle.collectAsStateWithLifecycle()
    val strokes by viewModel.strokes.collectAsStateWithLifecycle()
    val textBlocks by viewModel.textBlocks.collectAsStateWithLifecycle()
    val activeTool by viewModel.activeTool.collectAsStateWithLifecycle()
    val activeInkColor by viewModel.activeInkColor.collectAsStateWithLifecycle()
    val penWidth by viewModel.penWidth.collectAsStateWithLifecycle()
    val eraserRadius by viewModel.eraserRadius.collectAsStateWithLifecycle()
    val paperTheme by viewModel.paperTheme.collectAsStateWithLifecycle()
    val paperPattern by viewModel.paperPattern.collectAsStateWithLifecycle()
    val stylusOnlyMode by viewModel.stylusOnlyMode.collectAsStateWithLifecycle()
    val isStylusDetected by viewModel.isStylusDetected.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val pdfExportState by viewModel.pdfExportState.collectAsStateWithLifecycle()
    val pngExportState by viewModel.pngExportState.collectAsStateWithLifecycle()

    // Canvas Freeform Zoom & Pan
    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var showTitleDialog by remember { mutableStateOf(false) }
    var showPaperDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showPenWidthPopover by remember { mutableStateOf(false) }

    // Text block interaction dialogs
    var showAddTextDialog by remember { mutableStateOf(false) }
    var textTapPosition by remember { mutableStateOf(Offset(100f, 200f)) }
    var editingTextBlock by remember { mutableStateOf<TextBlock?>(null) }

    // Live stroke point collector for lag-free, 90Hz drawing
    val livePoints = remember { mutableStateListOf<PointData>() }
    var liveEraserCursor by remember { mutableStateOf<Offset?>(null) }
    var canvasDimensions by remember { mutableStateOf(Pair(1080f, 1920f)) }

    fun screenToCanvas(screenPos: Offset): Offset {
        return Offset(
            x = (screenPos.x - panOffset.x) / zoom,
            y = (screenPos.y - panOffset.y) / zoom
        )
    }

    // File saver launcher for PNG direct export
    val savePngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        viewModel.exportPngToStream(
                            outputStream = stream,
                            width = canvasDimensions.first,
                            height = canvasDimensions.second
                        ) { success ->
                            if (success) {
                                viewModel.dismissPngExportDialog()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Handled
                }
            }
        }
    }

    // File saver launcher for PDF direct export
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        viewModel.exportPdfToStream(
                            outputStream = stream,
                            width = canvasDimensions.first,
                            height = canvasDimensions.second
                        ) { success ->
                            if (success) {
                                viewModel.dismissPdfExportDialog()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Handled
                }
            }
        }
    }

    BackHandler {
        viewModel.saveCurrentNote()
        onNavigateBack()
    }

    // PDF Export Completed Dialog
    when (val state = pdfExportState) {
        is PdfExportState.Ready -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissPdfExportDialog() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PDF Export Ready")
                    }
                },
                text = {
                    Column {
                        Text("Your note '${state.title}' has been rendered into a crisp vector PDF document.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Size: ${state.file.length() / 1024} KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val shareIntent = PdfExporter.createSharePdfIntent(context, state.file, state.title)
                            context.startActivity(Intent.createChooser(shareIntent, "Share or Save Note PDF"))
                            viewModel.dismissPdfExportDialog()
                        },
                        modifier = Modifier.testTag("share_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share / Save PDF")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissPdfExportDialog() }) {
                        Text("Done")
                    }
                }
            )
        }
        is PdfExportState.Generating -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("Exporting PDF...") },
                text = { Text("Rendering vector strokes and text at high resolution...") }
            )
        }
        is PdfExportState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissPdfExportDialog() },
                title = { Text("Export Failed") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissPdfExportDialog() }) {
                        Text("OK")
                    }
                }
            )
        }
        PdfExportState.Idle -> {}
    }

    // PNG Export Completed Dialog
    when (val state = pngExportState) {
        is PngExportState.Ready -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissPngExportDialog() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PNG Image Export Ready")
                    }
                },
                text = {
                    Column {
                        Text("Your sketch '${state.title}' has been rendered into a high-resolution PNG image.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Size: ${state.file.length() / 1024} KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val shareIntent = ImageExporter.createSharePngIntent(context, state.file, state.title)
                            context.startActivity(Intent.createChooser(shareIntent, "Share or Save Note Image"))
                            viewModel.dismissPngExportDialog()
                        },
                        modifier = Modifier.testTag("share_png_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share / Save Image")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissPngExportDialog() }) {
                        Text("Done")
                    }
                }
            )
        }
        is PngExportState.Generating -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("Exporting PNG Image...") },
                text = { Text("Rendering canvas and strokes at high resolution...") }
            )
        }
        is PngExportState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissPngExportDialog() },
                title = { Text("Export Failed") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissPngExportDialog() }) {
                        Text("OK")
                    }
                }
            )
        }
        PngExportState.Idle -> {}
    }

    // Edit Title Dialog
    if (showTitleDialog) {
        var tempTitle by remember { mutableStateOf(noteTitle) }
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            title = { Text("Note Title") },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    singleLine = true,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setNoteTitle(tempTitle.ifBlank { "Untitled Note" })
                        showTitleDialog = false
                    },
                    modifier = Modifier.testTag("save_title_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTitleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Canvas Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Canvas?") },
            text = { Text("This will erase all drawing strokes and text blocks on this page. You can still use Undo.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCanvas()
                        showClearConfirmDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_button")
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Paper Style Dialog
    if (showPaperDialog) {
        PaperStyleDialog(
            currentTheme = paperTheme,
            currentPattern = paperPattern,
            onThemeSelected = { viewModel.setPaperTheme(it) },
            onPatternSelected = { viewModel.setPaperPattern(it) },
            onDismiss = { showPaperDialog = false }
        )
    }

    // Add / Edit Text Block Dialog
    if (showAddTextDialog || editingTextBlock != null) {
        val targetBlock = editingTextBlock
        var blockContent by remember(targetBlock) { mutableStateOf(targetBlock?.text ?: "") }
        var fontSizeSp by remember(targetBlock) { mutableFloatStateOf(targetBlock?.fontSizeSp ?: 18f) }

        AlertDialog(
            onDismissRequest = {
                showAddTextDialog = false
                editingTextBlock = null
            },
            title = { Text(if (targetBlock != null) "Edit Text Block" else "Add Text Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = blockContent,
                        onValueChange = { blockContent = it },
                        label = { Text("Enter text") },
                        placeholder = { Text("Type note...") },
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().testTag("text_block_input")
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Font size: ${fontSizeSp.roundToInt()}sp", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = fontSizeSp,
                            onValueChange = { fontSizeSp = it },
                            valueRange = 12f..36f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetBlock != null) {
                            viewModel.updateTextBlock(targetBlock.id, blockContent, targetBlock.x, targetBlock.y)
                        } else {
                            viewModel.addTextBlock(
                                text = blockContent,
                                x = textTapPosition.x,
                                y = textTapPosition.y,
                                fontSizeSp = fontSizeSp
                            )
                        }
                        showAddTextDialog = false
                        editingTextBlock = null
                    },
                    modifier = Modifier.testTag("save_text_block_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row {
                    if (targetBlock != null) {
                        TextButton(
                            onClick = {
                                viewModel.deleteTextBlock(targetBlock.id)
                                showAddTextDialog = false
                                editingTextBlock = null
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(
                        onClick = {
                            showAddTextDialog = false
                            editingTextBlock = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.saveCurrentNote()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showTitleDialog = true }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        Text(
                            text = noteTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Title",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                actions = {
                    // Undo & Redo
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = canUndo,
                        modifier = Modifier.testTag("undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = canRedo,
                        modifier = Modifier.testTag("redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Stylus / Palm Rejection Indicator Chip
                    FilterChip(
                        selected = stylusOnlyMode,
                        onClick = { viewModel.toggleStylusOnlyMode() },
                        label = {
                            Text(
                                text = if (stylusOnlyMode) "Stylus Only" else "Touch+Pen",
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("palm_rejection_chip")
                    )

                    // Overflow Menu
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Paper & Background Style") },
                                leadingIcon = { Icon(Icons.Default.FormatPaint, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    showPaperDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as PNG Image") },
                                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.exportToPng(canvasDimensions.first, canvasDimensions.second)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.exportToPdf(canvasDimensions.first, canvasDimensions.second)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reset View / Zoom (100%)") },
                                leadingIcon = { Icon(Icons.Default.CenterFocusStrong, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    zoom = 1f
                                    panOffset = Offset.Zero
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Canvas") },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    showClearConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(paperTheme.backgroundHex))
        ) {
            canvasDimensions = Pair(maxWidth.value * 2.5f, maxHeight.value * 2.5f)

            // Canvas drawing layer
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("drawing_canvas")
                    .pointerInput(activeTool, stylusOnlyMode, activeInkColor, penWidth, eraserRadius, zoom, panOffset) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            showPenWidthPopover = false
                            val isStylus = down.type == PointerType.Stylus || down.type == PointerType.Eraser
                            if (isStylus) {
                                viewModel.onStylusInputDetected()
                            }

                            // Stylus-Only Mode: Touch gestures pan and zoom the canvas freely!
                            if (stylusOnlyMode && !isStylus) {
                                var current = down
                                while (current.pressed) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.size >= 2) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        zoom = (zoom * zoomChange).coerceIn(0.4f, 5.0f)
                                        panOffset += panChange
                                        event.changes.forEach { it.consume() }
                                    } else {
                                        val moveChange = event.changes.firstOrNull { it.id == current.id }
                                        if (moveChange != null && moveChange.pressed) {
                                            val delta = moveChange.position - current.position
                                            panOffset += delta
                                            moveChange.consume()
                                            current = moveChange
                                        } else {
                                            break
                                        }
                                    }
                                }
                                return@awaitEachGesture
                            }

                            val canvasDown = screenToCanvas(down.position)

                            when (activeTool) {
                                ActiveTool.PEN -> {
                                    livePoints.clear()
                                    val startPressure = if (isStylus) down.pressure else 1.0f
                                    livePoints.add(PointData(canvasDown.x, canvasDown.y, startPressure))

                                    var current = down
                                    while (current.pressed) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.size >= 2) {
                                            // Pinch to zoom in / zoom out and freeform pan with 2 fingers
                                            livePoints.clear()
                                            val zoomChange = event.calculateZoom()
                                            val panChange = event.calculatePan()
                                            zoom = (zoom * zoomChange).coerceIn(0.4f, 5.0f)
                                            panOffset += panChange
                                            event.changes.forEach { it.consume() }
                                        } else {
                                            val moveChange = event.changes.firstOrNull { it.id == current.id }
                                            if (moveChange != null && moveChange.pressed) {
                                                val cPos = screenToCanvas(moveChange.position)
                                                val p = if (isStylus) moveChange.pressure else 1.0f
                                                livePoints.add(PointData(cPos.x, cPos.y, p))
                                                moveChange.consume()
                                                current = moveChange
                                            } else {
                                                break
                                            }
                                        }
                                    }

                                    if (livePoints.size >= 1) {
                                        val strokePoints = livePoints.toList()
                                        val effectiveWidth = if (strokePoints.isNotEmpty()) {
                                            DrawingEngine.calculateEffectiveWidth(penWidth, strokePoints.last().pressure)
                                        } else penWidth

                                        val newStroke = DrawingStroke(
                                            points = strokePoints,
                                            colorHex = activeInkColor,
                                            strokeWidth = effectiveWidth
                                        )
                                        viewModel.addStroke(newStroke)
                                    }
                                    livePoints.clear()
                                }

                                ActiveTool.ERASER -> {
                                    liveEraserCursor = down.position
                                    viewModel.eraseStrokesAt(canvasDown, eraserRadius / zoom)

                                    var current = down
                                    while (current.pressed) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.size >= 2) {
                                            val zoomChange = event.calculateZoom()
                                            val panChange = event.calculatePan()
                                            zoom = (zoom * zoomChange).coerceIn(0.4f, 5.0f)
                                            panOffset += panChange
                                            event.changes.forEach { it.consume() }
                                        } else {
                                            val moveChange = event.changes.firstOrNull { it.id == current.id }
                                            if (moveChange != null && moveChange.pressed) {
                                                val cPos = screenToCanvas(moveChange.position)
                                                liveEraserCursor = moveChange.position
                                                viewModel.eraseStrokesAt(cPos, eraserRadius / zoom)
                                                moveChange.consume()
                                                current = moveChange
                                            } else {
                                                break
                                            }
                                        }
                                    }
                                    liveEraserCursor = null
                                }

                                ActiveTool.TEXT -> {
                                    textTapPosition = canvasDown
                                    showAddTextDialog = true
                                }
                            }
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Apply zoom and freeform pan transformation
                withTransform({
                    translate(panOffset.x, panOffset.y)
                    scale(zoom, zoom, Offset.Zero)
                }) {
                    // 1. Draw Paper Pattern (Ruled lines, Grid, Dots)
                    val linePaintColor = Color(paperTheme.gridLineHex)
                    val virtualWidth = maxOf(canvasWidth, 3000f)
                    val virtualHeight = maxOf(canvasHeight, 4000f)

                    when (paperPattern) {
                        PaperPattern.RULED -> {
                            val spacing = 32.dp.toPx()
                            var y = -2000f
                            while (y < virtualHeight) {
                                drawLine(
                                    color = linePaintColor,
                                    start = Offset(-2000f, y),
                                    end = Offset(virtualWidth, y),
                                    strokeWidth = 1f
                                )
                                y += spacing
                            }
                        }
                        PaperPattern.GRID -> {
                            val gridSize = 24.dp.toPx()
                            var x = -2000f
                            while (x < virtualWidth) {
                                drawLine(
                                    color = linePaintColor,
                                    start = Offset(x, -2000f),
                                    end = Offset(x, virtualHeight),
                                    strokeWidth = 1f
                                )
                                x += gridSize
                            }
                            var y = -2000f
                            while (y < virtualHeight) {
                                drawLine(
                                    color = linePaintColor,
                                    start = Offset(-2000f, y),
                                    end = Offset(virtualWidth, y),
                                    strokeWidth = 1f
                                )
                                y += gridSize
                            }
                        }
                        PaperPattern.DOTS -> {
                            val dotSpacing = 24.dp.toPx()
                            var x = -1000f
                            while (x < virtualWidth) {
                                var y = -1000f
                                while (y < virtualHeight) {
                                    drawCircle(
                                        color = linePaintColor,
                                        radius = 1.4f,
                                        center = Offset(x, y)
                                    )
                                    y += dotSpacing
                                }
                                x += dotSpacing
                            }
                        }
                        PaperPattern.BLANK -> { /* Clean canvas */ }
                    }

                    // 2. Draw Committed Strokes
                    for (stroke in strokes) {
                        if (stroke.points.size < 2) continue
                        val path = DrawingEngine.createSmoothPath(stroke.points)
                        drawPath(
                            path = path,
                            color = Color(stroke.colorHex),
                            style = Stroke(
                                width = stroke.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // 3. Draw Active Live Stroke in Progress (Lag-Free Feedback)
                    if (livePoints.size >= 2) {
                        val livePath = DrawingEngine.createSmoothPath(livePoints)
                        drawPath(
                            path = livePath,
                            color = Color(activeInkColor),
                            style = Stroke(
                                width = penWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // 4. Draw Eraser Cursor Indicator in screen space
                liveEraserCursor?.let { cursor ->
                    drawCircle(
                        color = Color(0x33EF4444),
                        radius = eraserRadius,
                        center = cursor
                    )
                    drawCircle(
                        color = Color(0xFFEF4444),
                        radius = eraserRadius,
                        center = cursor,
                        style = Stroke(width = 2f)
                    )
                }
            }

            // Text Blocks Overlay with Zoom & Pan coordinate transformation
            for (block in textBlocks) {
                val blockScreenX = block.x * zoom + panOffset.x
                val blockScreenY = block.y * zoom + panOffset.y

                Box(
                    modifier = Modifier
                        .offset { IntOffset(blockScreenX.roundToInt(), blockScreenY.roundToInt()) }
                        .pointerInput(block.id, zoom, panOffset) {
                            detectDragGestures(
                                onDragEnd = { /* auto-saved in repo */ },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newCanvasX = block.x + (dragAmount.x / zoom)
                                    val newCanvasY = block.y + (dragAmount.y / zoom)
                                    viewModel.updateTextBlock(block.id, block.text, newCanvasX, newCanvasY)
                                }
                            )
                        }
                        .clickable { editingTextBlock = block }
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Transparent)
                ) {
                    Text(
                        text = block.text,
                        fontSize = (block.fontSizeSp * zoom).sp,
                        fontWeight = if (block.isBold) FontWeight.Bold else FontWeight.Normal,
                        color = Color(block.colorHex),
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }

            // Floating Zoom & Freeform Navigation Controls on Top-Right
            FloatingZoomControls(
                zoom = zoom,
                onZoomIn = { zoom = (zoom * 1.25f).coerceAtMost(4.0f) },
                onZoomOut = { zoom = (zoom / 1.25f).coerceAtLeast(0.4f) },
                onResetZoom = {
                    zoom = 1f
                    panOffset = Offset.Zero
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
            )

            // Stroke Width Popover floating directly above the bottom dock
            AnimatedVisibility(
                visible = showPenWidthPopover && activeTool == ActiveTool.PEN,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 86.dp)
            ) {
                StrokeWidthPopover(
                    penWidth = penWidth,
                    activeInkColor = activeInkColor,
                    onPenWidthSelected = { viewModel.setPenWidth(it) },
                    onDismiss = { showPenWidthPopover = false }
                )
            }

            // Minimalist Floating Dock at the Bottom
            MinimalistToolDock(
                activeTool = activeTool,
                activeInkColor = activeInkColor,
                penWidth = penWidth,
                eraserRadius = eraserRadius,
                isPenWidthPopoverOpen = showPenWidthPopover,
                onToolSelected = { tool ->
                    if (tool == ActiveTool.PEN && activeTool == ActiveTool.PEN) {
                        showPenWidthPopover = !showPenWidthPopover
                    } else {
                        showPenWidthPopover = false
                        viewModel.selectTool(tool)
                    }
                },
                onToggleWidthPopover = {
                    showPenWidthPopover = !showPenWidthPopover
                },
                onColorSelected = { viewModel.selectInkColor(it) },
                onPenWidthSelected = { viewModel.setPenWidth(it) },
                onEraserRadiusSelected = { viewModel.setEraserRadius(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            )
        }
    }
}

/**
 * Minimalist floating dock containing only:
 * - 3 Tools: Pen, Eraser, Text
 * - 4 Primary Ink Colors: Red, Blue, White, Black
 * - Size settings & Stroke Width slider button
 */
@Composable
fun MinimalistToolDock(
    activeTool: ActiveTool,
    activeInkColor: Long,
    penWidth: Float,
    eraserRadius: Float,
    isPenWidthPopoverOpen: Boolean,
    onToolSelected: (ActiveTool) -> Unit,
    onToggleWidthPopover: () -> Unit,
    onColorSelected: (Long) -> Unit,
    onPenWidthSelected: (Float) -> Unit,
    onEraserRadiusSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(28.dp))
            .testTag("minimalist_tool_dock")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // --- 1. THE 3 TOOLS ONLY ---
                ToolButton(
                    icon = Icons.Default.Edit,
                    label = if (activeTool == ActiveTool.PEN) "${penWidth.toInt().coerceAtLeast(1)}px" else "Pen",
                    isSelected = activeTool == ActiveTool.PEN,
                    onClick = { onToolSelected(ActiveTool.PEN) },
                    tag = "tool_pen"
                )

                ToolButton(
                    icon = Icons.Default.AutoFixNormal,
                    label = "Eraser",
                    isSelected = activeTool == ActiveTool.ERASER,
                    onClick = { onToolSelected(ActiveTool.ERASER) },
                    tag = "tool_eraser"
                )

                ToolButton(
                    icon = Icons.Default.TextFields,
                    label = "Text",
                    isSelected = activeTool == ActiveTool.TEXT,
                    onClick = { onToolSelected(ActiveTool.TEXT) },
                    tag = "tool_text"
                )

                // Divider
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )

                // --- 2. THE 4 PRIMARY INK COLORS ONLY ---
                // Black, Blue, Red, White
                for (option in InkColors.ALL) {
                    val isSelected = activeInkColor == option.hexValue
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                            .clickable { onColorSelected(option.hexValue) }
                            .testTag("color_${option.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(option.previewColor))
                                .border(
                                    width = if (option.hexValue == InkColors.WHITE_HEX) 1.5.dp else 0.dp,
                                    color = Color(0xFFCBD5E1),
                                    shape = CircleShape
                                )
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (option.hexValue == InkColors.WHITE_HEX) Color.Black else Color.White,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )

                // --- 3. SIZE SETTINGS & SLIDER ACCESS ---
                if (activeTool == ActiveTool.ERASER) {
                    // Small / Medium / Large Eraser
                    SizePresetDot(sizeDp = 10, isSelected = eraserRadius < 20f) { onEraserRadiusSelected(16f) }
                    SizePresetDot(sizeDp = 16, isSelected = eraserRadius in 20f..35f) { onEraserRadiusSelected(28f) }
                    SizePresetDot(sizeDp = 22, isSelected = eraserRadius > 35f) { onEraserRadiusSelected(48f) }
                } else {
                    // Stroke width customization trigger button with slider tune icon
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPenWidthPopoverOpen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onToggleWidthPopover)
                            .testTag("open_stroke_slider_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Customize Pen Stroke Width",
                                modifier = Modifier.size(16.dp),
                                tint = if (isPenWidthPopoverOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Quick dot presets: Fine (2.5dp), Normal (5.5dp), Bold (12dp)
                    SizePresetDot(sizeDp = 6, isSelected = penWidth <= 3.5f) { onPenWidthSelected(2.5f) }
                    SizePresetDot(sizeDp = 11, isSelected = penWidth in 3.6f..8f) { onPenWidthSelected(5.5f) }
                    SizePresetDot(sizeDp = 18, isSelected = penWidth > 8f) { onPenWidthSelected(12f) }
                }
            }
        }
    }
}

/**
 * Floating Popover Panel for customizing the pen stroke width with a slider,
 * live ink preview line, and quick thickness presets.
 */
@Composable
fun StrokeWidthPopover(
    penWidth: Float,
    activeInkColor: Long,
    onPenWidthSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = modifier
            .widthIn(min = 290.dp, max = 340.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .testTag("stroke_width_popover")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title, value badge, and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Stroke Width",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = "${penWidth.toInt().coerceAtLeast(1)} px",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("close_popover_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Live Ink Stroke Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerY = size.height / 2f
                    val startX = 28.dp.toPx()
                    val endX = size.width - 28.dp.toPx()

                    // Render dynamic live line matching exact selected ink color and thickness
                    drawLine(
                        color = Color(activeInkColor),
                        start = Offset(startX, centerY),
                        end = Offset(endX, centerY),
                        strokeWidth = penWidth.coerceIn(1f, 32f),
                        cap = StrokeCap.Round
                    )

                    // If ink is white, draw subtle edge marks so it is clearly visible on light surfaces
                    if (activeInkColor == InkColors.WHITE_HEX) {
                        drawCircle(
                            color = Color(0xFF94A3B8),
                            radius = (penWidth / 2f + 1f).coerceAtLeast(3f),
                            center = Offset(startX, centerY),
                            style = Stroke(width = 1f)
                        )
                        drawCircle(
                            color = Color(0xFF94A3B8),
                            radius = (penWidth / 2f + 1f).coerceAtLeast(3f),
                            center = Offset(endX, centerY),
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }

            // Continuous Slider (1px to 30px)
            Column {
                Slider(
                    value = penWidth,
                    onValueChange = { onPenWidthSelected(it) },
                    valueRange = 1f..30f,
                    steps = 28,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stroke_width_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "1 px (Hairline)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "30 px (Marker)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Presets Row (Fine, Regular, Medium, Bold, Marker)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val presets = listOf(
                    "Fine" to 2f,
                    "Regular" to 5f,
                    "Medium" to 10f,
                    "Bold" to 18f,
                    "Marker" to 28f
                )

                for ((label, size) in presets) {
                    val isSelected = (penWidth - size).let { it >= -1f && it <= 1f }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onPenWidthSelected(size) }
                            .testTag("preset_${label.lowercase()}")
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SizePresetDot(
    sizeDp: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
        )
    }
}

@Composable
fun PaperStyleDialog(
    currentTheme: PaperTheme,
    currentPattern: PaperPattern,
    onThemeSelected: (PaperTheme) -> Unit,
    onPatternSelected: (PaperPattern) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paper Style & Pattern") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Paper Color Theme",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (theme in PaperTheme.values()) {
                        val isSelected = theme == currentTheme
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                                )
                                .clickable { onThemeSelected(theme) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(theme.backgroundHex))
                                    .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = theme.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Text(
                    text = "Pattern",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (pattern in PaperPattern.values()) {
                        val isSelected = pattern == currentPattern
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPatternSelected(pattern) },
                            label = { Text(pattern.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

/**
 * Floating Zoom & Pan Navigation Controls
 * Allows quick zooming in/out, view percentage, and 1-tap view reset.
 */
@Composable
fun FloatingZoomControls(
    zoom: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = modifier.testTag("floating_zoom_controls")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("zoom_out_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = "Zoom Out",
                    modifier = Modifier.size(18.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onResetZoom)
                    .testTag("reset_zoom_button")
            ) {
                Text(
                    text = "${(zoom * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            IconButton(
                onClick = onZoomIn,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("zoom_in_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom In",
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onResetZoom,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("fit_view_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset Zoom to 100%",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
