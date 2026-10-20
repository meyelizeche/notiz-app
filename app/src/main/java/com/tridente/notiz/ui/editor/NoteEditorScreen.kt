package com.tridente.notiz.ui.editor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

import com.tridente.notiz.util.MarkdownParser
import com.tridente.notiz.util.MarkdownRenderView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onNavigateBack: () -> Unit,
    onOpenShareDialog: (noteId: String) -> Unit
) {
    val context = LocalContext.current

    val title by viewModel.title.collectAsState()
    val contentMarkdown by viewModel.contentMarkdown.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val dueDateTimestamp by viewModel.dueDateTimestamp.collectAsState()
    val currentNote by viewModel.currentNote.collectAsState()

    var isPreviewMode by remember { mutableStateOf(false) }

    var contentTextFieldValue by remember {
        mutableStateOf(TextFieldValue(contentMarkdown))
    }

    // Image Picker Launcher (Gallery / Camera)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addImageAttachment(context, it) }
    }

    // Date & Time Picker Dialogs for Due Date Alert
    fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        dueDateTimestamp?.let { calendar.timeInMillis = it }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)

                        viewModel.setDueDate(calendar.timeInMillis, context)
                        Toast.makeText(context, "Recordatorio programado", Toast.LENGTH_SHORT).show()
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentNote == null) "Nueva Nota" else "Editar Nota") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    // Preview Toggle
                    IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Icon(
                            if (isPreviewMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Vista previa Markdown"
                        )
                    }

                    // Camera / Photo Picker
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Adjuntar Imagen")
                    }

                    // Due Date / Calendar Picker
                    IconButton(onClick = { showDateTimePicker() }) {
                        Icon(Icons.Default.Event, contentDescription = "Fecha Límite")
                    }

                    // Export PDF / Print
                    IconButton(onClick = {
                        val pdfUri = viewModel.exportToPdf(context)
                        if (pdfUri != null) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, pdfUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir Documento PDF"))
                        } else {
                            Toast.makeText(context, "Error al generar PDF", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF")
                    }

                    // Print
                    IconButton(onClick = { viewModel.printNote(context) }) {
                        Icon(Icons.Default.Print, contentDescription = "Imprimir")
                    }

                    // Share
                    IconButton(onClick = {
                        currentNote?.id?.let { noteId ->
                            onOpenShareDialog(noteId)
                        } ?: run {
                            viewModel.saveNote {
                                viewModel.currentNote.value?.id?.let { noteId ->
                                    onOpenShareDialog(noteId)
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir")
                    }

                    // Save Note
                    IconButton(onClick = {
                        viewModel.saveNote {
                            Toast.makeText(context, "Nota guardada", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.title.value = it },
                    placeholder = { Text("Título de la nota...", style = MaterialTheme.typography.headlineSmall) },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Due Date Chip & Calendar Sync Button
                dueDateTimestamp?.let { timestamp ->
                    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            onClick = { showDateTimePicker() },
                            label = { Text("Límite: ${sdf.format(Date(timestamp))}") },
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        AssistChip(
                            onClick = { viewModel.syncWithNativeCalendar(context) },
                            label = { Text("Añadir a Calendario") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Attachment Images Section
                AttachmentSection(
                    attachments = attachments,
                    onDeleteAttachment = { viewModel.deleteAttachment(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // WYSIWYG Content Editor or Markdown Preview
                if (isPreviewMode) {
                    Text(
                        text = "--- Vista Previa Markdown ---",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    MarkdownRenderView(
                        markdownText = contentMarkdown,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = contentTextFieldValue,
                        onValueChange = {
                            contentTextFieldValue = it
                            viewModel.contentMarkdown.value = it.text
                        },
                        placeholder = { Text("Escribe tu nota en estándar Markdown (UTF-8)...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    )
                }
            }

            // Quick Formatting Toolbar
            if (!isPreviewMode) {
                MarkdownToolbar(
                    onHeaderClick = { level ->
                        val updated = MarkdownParser.applyHeader(contentTextFieldValue.text, level)
                        contentTextFieldValue = TextFieldValue(updated, TextRange(updated.length))
                        viewModel.contentMarkdown.value = updated
                    },
                    onBoldClick = {
                        val selectionStart = contentTextFieldValue.selection.start
                        val selectionEnd = contentTextFieldValue.selection.end
                        val updated = MarkdownParser.applyBold(contentTextFieldValue.text, selectionStart, selectionEnd)
                        contentTextFieldValue = TextFieldValue(updated, TextRange(updated.length))
                        viewModel.contentMarkdown.value = updated
                    },
                    onItalicClick = {
                        val selectionStart = contentTextFieldValue.selection.start
                        val selectionEnd = contentTextFieldValue.selection.end
                        val updated = MarkdownParser.applyItalic(contentTextFieldValue.text, selectionStart, selectionEnd)
                        contentTextFieldValue = TextFieldValue(updated, TextRange(updated.length))
                        viewModel.contentMarkdown.value = updated
                    },
                    onListClick = {
                        val updated = MarkdownParser.applyList(contentTextFieldValue.text)
                        contentTextFieldValue = TextFieldValue(updated, TextRange(updated.length))
                        viewModel.contentMarkdown.value = updated
                    },
                    onChecklistClick = {
                        val updated = MarkdownParser.applyChecklist(contentTextFieldValue.text)
                        contentTextFieldValue = TextFieldValue(updated, TextRange(updated.length))
                        viewModel.contentMarkdown.value = updated
                    },
                    onCodeClick = {
                        val updated = MarkdownParser.applyCodeBlock(contentTextFieldValue.text)
                        contentTextFieldValue = TextFieldValue(updated, TextRange(updated.length))
                        viewModel.contentMarkdown.value = updated
                    }
                )
            }
        }
    }
}
