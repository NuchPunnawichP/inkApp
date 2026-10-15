package com.example.drawing

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.DrawingStroke
import com.example.data.model.PaperPattern
import com.example.data.model.PaperTheme
import com.example.data.model.TextBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object PdfExporter {

    private const val PAGE_WIDTH = 595 // A4 width in pt
    private const val PAGE_HEIGHT = 842 // A4 height in pt

    suspend fun generatePdf(
        context: Context,
        title: String,
        strokes: List<DrawingStroke>,
        textBlocks: List<TextBlock>,
        paperTheme: PaperTheme,
        paperPattern: PaperPattern,
        canvasWidth: Float,
        canvasHeight: Float,
        outputStream: OutputStream? = null
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Determine scale factor between screen canvas and A4 page
        val safeCanvasWidth = if (canvasWidth > 100f) canvasWidth else PAGE_WIDTH.toFloat()
        val safeCanvasHeight = if (canvasHeight > 100f) canvasHeight else PAGE_HEIGHT.toFloat()

        val scaleX = PAGE_WIDTH.toFloat() / safeCanvasWidth
        val scaleY = PAGE_HEIGHT.toFloat() / safeCanvasHeight
        val scale = minOf(scaleX, scaleY).coerceIn(0.2f, 2.5f)

        // Draw Paper Background
        val bgPaint = Paint().apply {
            color = paperTheme.backgroundHex.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

        // Draw Paper Pattern
        val patternPaint = Paint().apply {
            color = paperTheme.gridLineHex.toInt()
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        when (paperPattern) {
            PaperPattern.RULED -> {
                val lineSpacing = 32f * scale
                var y = 60f * scale
                while (y < PAGE_HEIGHT) {
                    canvas.drawLine(0f, y, PAGE_WIDTH.toFloat(), y, patternPaint)
                    y += lineSpacing
                }
            }
            PaperPattern.GRID -> {
                val gridSize = 24f * scale
                var x = 0f
                while (x < PAGE_WIDTH) {
                    canvas.drawLine(x, 0f, x, PAGE_HEIGHT.toFloat(), patternPaint)
                    x += gridSize
                }
                var y = 0f
                while (y < PAGE_HEIGHT) {
                    canvas.drawLine(0f, y, PAGE_WIDTH.toFloat(), y, patternPaint)
                    y += gridSize
                }
            }
            PaperPattern.DOTS -> {
                val dotSize = 24f * scale
                val dotPaint = Paint().apply {
                    color = paperTheme.gridLineHex.toInt()
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                var x = dotSize
                while (x < PAGE_WIDTH) {
                    var y = dotSize
                    while (y < PAGE_HEIGHT) {
                        canvas.drawCircle(x, y, 1.2f, dotPaint)
                        y += dotSize
                    }
                    x += dotSize
                }
            }
            PaperPattern.BLANK -> { /* Clean canvas */ }
        }

        // Draw Header: Title & Date (Subtle elegant header)
        val headerTitlePaint = Paint().apply {
            color = if (paperTheme.isDark) Color.parseColor("#94A3B8") else Color.parseColor("#475569")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        canvas.drawText(title.take(30), 24f, 28f, headerTitlePaint)

        val headerDatePaint = Paint().apply {
            color = if (paperTheme.isDark) Color.parseColor("#64748B") else Color.parseColor("#94A3B8")
            textSize = 10f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("InkNote • $dateStr", PAGE_WIDTH - 24f, 28f, headerDatePaint)

        // Draw Text Blocks
        for (tb in textBlocks) {
            val textPaint = Paint().apply {
                color = tb.colorHex.toInt()
                textSize = tb.fontSizeSp * scale * 1.15f
                typeface = if (tb.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                isAntiAlias = true
            }

            val lines = tb.text.split("\n")
            var lineY = tb.y * scale
            val lineHeight = textPaint.textSize * 1.3f

            for (line in lines) {
                canvas.drawText(line, tb.x * scale, lineY, textPaint)
                lineY += lineHeight
            }
        }

        // Draw Drawing Strokes
        val strokePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        for (stroke in strokes) {
            if (stroke.points.size < 2) continue

            strokePaint.color = stroke.colorHex.toInt()
            strokePaint.strokeWidth = stroke.strokeWidth * scale

            val path = android.graphics.Path()
            val points = stroke.points

            path.moveTo(points[0].x * scale, points[0].y * scale)

            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val midX = (prev.x + curr.x) / 2f * scale
                val midY = (prev.y + curr.y) / 2f * scale

                if (i == 1) {
                    path.lineTo(midX, midY)
                } else {
                    path.quadTo(prev.x * scale, prev.y * scale, midX, midY)
                }
            }
            val last = points.last()
            path.lineTo(last.x * scale, last.y * scale)

            canvas.drawPath(path, strokePaint)
        }

        document.finishPage(page)

        // Write to target
        val pdfDir = File(context.cacheDir, "pdf").apply { mkdirs() }
        val safeFileName = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Note" }
        val pdfFile = File(pdfDir, "${safeFileName}_${System.currentTimeMillis()}.pdf")

        if (outputStream != null) {
            document.writeTo(outputStream)
        } else {
            FileOutputStream(pdfFile).use { fos ->
                document.writeTo(fos)
            }
        }

        document.close()
        pdfFile
    }

    fun getFileUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun createSharePdfIntent(context: Context, file: File, title: String): Intent {
        val uri = getFileUri(context, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Exported note from InkNote on Vivo V23 5G")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
