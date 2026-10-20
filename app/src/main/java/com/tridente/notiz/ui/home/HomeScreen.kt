package com.tridente.notiz.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridente.notiz.data.local.entities.FolderEntity
import com.tridente.notiz.ui.folder.FolderDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToEditor: (noteId: String?) -> Unit,
    onOpenShareDialog: (noteId: String) -> Unit,
    onCreateFolderClick: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    val notes by viewModel.notes.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var showFolderDialog by remember { mutableStateOf(false) }
    var moveNoteId by remember { mutableStateOf<String?>(null) }

    val currentFolderName = remember(selectedFolderId, folders) {
        folders.find { it.id == selectedFolderId }?.name ?: "Todas las Notas"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FolderDrawer(
                currentUser = currentUser,
                folders = folders,
                selectedFolderId = selectedFolderId,
                onSelectFolder = { folderId ->
                    viewModel.selectFolder(folderId)
                    scope.launch { drawerState.close() }
                },
                onCreateFolderClick = { showFolderDialog = true },
                onLogoutClick = { viewModel.logout() }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentFolderName, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú de carpetas")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.triggerSync() }) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = "Sincronizar ahora")
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNavigateToEditor(null) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva Nota")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchNotes(it) },
                    placeholder = { Text("Buscar notas por título o contenido...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchNotes("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (notes.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        Text(
                            text = "No hay notas guardadas aún.\n¡Presiona + para crear una!",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteItem(
                                note = note,
                                onClick = { onNavigateToEditor(note.id) },
                                onDeleteClick = { viewModel.deleteNote(note.id) },
                                onShareClick = { onOpenShareDialog(note.id) },
                                onMoveClick = { moveNoteId = note.id }
                            )
                        }
                    }
                }
            }
        }
    }

    // New Folder Dialog
    if (showFolderDialog) {
        FolderDialog(
            folders = folders,
            onDismiss = { showFolderDialog = false },
            onCreateFolder = { name, parentId ->
                onCreateFolderClick()
            }
        )
    }

    // Move Note To Folder Dialog
    moveNoteId?.let { noteId ->
        AlertDialog(
            onDismissRequest = { moveNoteId = null },
            title = { Text("Mover Nota a Carpeta") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            viewModel.moveNoteToFolder(noteId, null)
                            moveNoteId = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📁 Sin Carpeta (Raíz)")
                    }
                    folders.forEach { folder ->
                        TextButton(
                            onClick = {
                                viewModel.moveNoteToFolder(noteId, folder.id)
                                moveNoteId = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📁 ${folder.name}")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { moveNoteId = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
