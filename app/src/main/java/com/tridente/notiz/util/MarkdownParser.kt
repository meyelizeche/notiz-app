package com.tridente.notiz.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MarkdownParser {

    fun applyHeader(text: String, level: Int): String {
        val prefix = "#".repeat(level) + " "
        val lines = text.split("\n").toMutableList()
        if (lines.isNotEmpty()) {
            val firstLine = lines[0].removePrefix("#").trimStart()
            lines[0] = "$prefix$firstLine"
        }
        return lines.joinToString("\n")
    }

    fun applyBold(text: String, selectionStart: Int, selectionEnd: Int): String {
        return wrapSelection(text, selectionStart, selectionEnd, "**", "**")
    }

    fun applyItalic(text: String, selectionStart: Int, selectionEnd: Int): String {
        return wrapSelection(text, selectionStart, selectionEnd, "*", "*")
    }

    fun applyList(text: String): String {
        return text + "\n- "
    }

    fun applyChecklist(text: String): String {
        return text + "\n- [ ] "
    }

    fun applyCodeBlock(text: String): String {
        return text + "\n```\n\n```"
    }

    private fun wrapSelection(
        text: String,
        start: Int,
        end: Int,
        prefix: String,
        suffix: String
    ): String {
        val safeStart = start.coerceIn(0, text.length)
        val safeEnd = end.coerceIn(0, text.length)

        val before = text.substring(0, safeStart.coerceAtMost(safeEnd))
        val selected = text.substring(safeStart.coerceAtMost(safeEnd), safeStart.coerceAtLeast(safeEnd))
        val after = text.substring(safeStart.coerceAtLeast(safeEnd))

        val innerText = if (selected.isEmpty()) "texto" else selected
        return "$before$prefix$innerText$suffix$after"
    }
}

@Composable
fun MarkdownRenderView(
    markdownText: String,
    modifier: Modifier = Modifier
) {
    val lines = markdownText.split("\n")

    Column(modifier = modifier.fillMaxWidth()) {
        lines.forEach { line ->
            when {
                line.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("# ").trim()),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("## ").trim()),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("### ").trim()),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                line.startsWith("- [ ] ") || line.startsWith("- [x] ") || line.startsWith("- [X] ") -> {
                    val isChecked = line.startsWith("- [x] ") || line.startsWith("- [X] ")
                    val checkText = line.substring(6)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = null,
                            enabled = false
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = parseInlineMarkdown(checkText),
                            textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "• ",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = parseInlineMarkdown(line.substring(2)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                line.startsWith("```") -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    if (line.isNotBlank()) {
                        Text(
                            text = parseInlineMarkdown(line),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

fun parseInlineMarkdown(text: String) = buildAnnotatedString {
    var cursor = 0
    val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
    val italicRegex = Regex("\\*(.*?)\\*")
    val codeRegex = Regex("`(.*?)`")

    var remainingText = text

    // Simplified inline bold / italic / code parser
    val parts = text.split("**")
    if (parts.size > 1) {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    } else {
        append(text)
    }
}
