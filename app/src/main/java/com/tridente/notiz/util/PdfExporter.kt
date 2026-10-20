package com.tridente.notiz.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    fun generatePdfFile(
        context: Context,
        title: String,
        contentMarkdown: String,
        imageBlobs: List<ByteArray>
    ): Uri? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size in points
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
            typeface = Typeface.DEFAULT
        }

        var yPosition = 50f
        canvas.drawText(title.ifBlank { "Sin Título" }, 40f, yPosition, titlePaint)
        yPosition += 40f

        val lines = contentMarkdown.split("\n")
        for (line in lines) {
            if (yPosition > 780f) break // Simple 1 page threshold for demo
            canvas.drawText(line, 40f, yPosition, bodyPaint)
            yPosition += 20f
        }

        // Draw image attachments if any
        for (blob in imageBlobs) {
            if (yPosition > 700f) break
            try {
                val bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.size)
                if (bitmap != null) {
                    val scaledWidth = 200f
                    val scaledHeight = (bitmap.height.toFloat() / bitmap.width.toFloat()) * scaledWidth
                    val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                    val destRect = android.graphics.RectF(40f, yPosition, 40f + scaledWidth, yPosition + scaledHeight)
                    canvas.drawBitmap(bitmap, srcRect, destRect, null)
                    yPosition += scaledHeight + 20f
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        document.finishPage(page)

        val pdfDir = File(context.cachePath(), "pdfs").apply { mkdirs() }
        val pdfFile = File(pdfDir, "Notiz_${System.currentTimeMillis()}.pdf")

        return try {
            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }

    private fun Context.cachePath(): File = cacheDir

    fun printNote(context: Context, title: String, contentMarkdown: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Notiz_${title.take(15)}"

        printManager.print(
            jobName,
            object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(jobName)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    val document = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas

                    val titlePaint = Paint().apply {
                        color = Color.BLACK
                        textSize = 20f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val bodyPaint = Paint().apply {
                        color = Color.DKGRAY
                        textSize = 12f
                    }

                    canvas.drawText(title, 40f, 50f, titlePaint)
                    var y = 80f
                    contentMarkdown.split("\n").forEach { line ->
                        canvas.drawText(line, 40f, y, bodyPaint)
                        y += 18f
                    }

                    document.finishPage(page)

                    try {
                        FileOutputStream(destination?.fileDescriptor).use { out ->
                            document.writeTo(out)
                        }
                        document.close()
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                        document.close()
                    }
                }
            },
            PrintAttributes.Builder().build()
        )
    }
}
