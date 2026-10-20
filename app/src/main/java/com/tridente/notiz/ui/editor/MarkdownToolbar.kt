package com.tridente.notiz.ui.editor

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownToolbar(
    onHeaderClick: (Int) -> Unit,
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onListClick: () -> Unit,
    onChecklistClick: () -> Unit,
    onCodeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            item {
                IconButton(onClick = { onHeaderClick(1) }) {
                    Icon(Icons.Default.Title, contentDescription = "Encabezado H1", tint = MaterialTheme.colorScheme.primary)
                }
            }
            item {
                IconButton(onClick = onBoldClick) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Negrita")
                }
            }
            item {
                IconButton(onClick = onItalicClick) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Cursiva")
                }
            }
            item {
                IconButton(onClick = onListClick) {
                    Icon(Icons.Default.FormatListBulleted, contentDescription = "Lista")
                }
            }
            item {
                IconButton(onClick = onChecklistClick) {
                    Icon(Icons.Default.PlaylistAddCheck, contentDescription = "Checklist")
                }
            }
            item {
                IconButton(onClick = onCodeClick) {
                    Icon(Icons.Default.Code, contentDescription = "Bloque de código")
                }
            }
        }
    }
}
