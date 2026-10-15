package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.DrawingCanvasScreen
import com.example.ui.screens.NoteListScreen
import com.example.ui.theme.InkNoteTheme
import com.example.ui.viewmodel.NoteViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            InkNoteTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InkNoteApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun InkNoteApp(viewModel: NoteViewModel) {
    val currentNoteId by viewModel.currentNoteId.collectAsStateWithLifecycle()

    Crossfade(targetState = currentNoteId, label = "screen_transition") { noteId ->
        if (noteId == null) {
            NoteListScreen(
                viewModel = viewModel,
                onNoteClick = { id ->
                    viewModel.openNote(id)
                },
                onNewNoteClick = {
                    viewModel.createNewNote()
                }
            )
        } else {
            DrawingCanvasScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    viewModel.closeCurrentNote()
                }
            )
        }
    }
}
