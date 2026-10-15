package com.example.drawing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
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

object ImageExporter {

    private const val DEFAULT_WIDTH = 1200
    private const val DEFAULT_HEIGHT = 1800

    suspend fun generatePng(
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
        val width = if (canvasWidth > 100f) canvasWidth.toInt() else DEFAULT_WIDTH
        val height = if (canvasHeight > 100f) canvasHeight.toInt() else DEFAULT_HEIGHT

        // Create high-resolution ARGB_8888 bitmap
        val bitmap = Bitmap.createBitmap(
            width.coerceIn(800, 2400),
            height.coerceIn(1200, 3600),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        // 1. Draw Paper Background
        val bgPaint = Paint().apply {
            color = paperTheme.backgroundHex.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), bgPaint)

        // 2. Draw Paper Pattern (Ruled, Grid, Dots)
        val patternPaint = Paint().apply {
            color = paperTheme.gridLineHex.toInt()
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        when (paperPattern) {
            PaperPattern.RULED -> {
                val lineSpacing = 42f
                var y = 80f
                while (y < bitmap.height) {
                    canvas.drawLine(0f, y, bitmap.width.toFloat(), y, patternPaint)
                    y += lineSpacing
                }
            }
            PaperPattern.GRID -> {
                val gridSize = 32f
                var x = 0f
                while (x < bitmap.width) {
                    canvas.drawLine(x, 0f, x, bitmap.height.toFloat(), patternPaint)
                    x += gridSize
                }
                var y = 0f
                while (y < bitmap.height) {
                    canvas.drawLine(0f, y, bitmap.width.toFloat(), y, patternPaint)
                    y += gridSize
                }
            }
            PaperPattern.DOTS -> {
                val dotSpacing = 32f
                val dotPaint = Paint().apply {
                    color = paperTheme.gridLineHex.toInt()
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                var x = dotSpacing
                while (x < bitmap.width) {
                    var y = dotSpacing
                    while (y < bitmap.height) {
                        canvas.drawCircle(x, y, 1.8f, dotPaint)
                        y += dotSpacing
                    }
                    x += dotSpacing
                }
            }
            PaperPattern.BLANK -> { /* Clean canvas */ }
        }

        // 3. Draw Watermark / Header
        val headerPaint = Paint().apply {
            color = if (paperTheme.isDark) Color.parseColor("#64748B") else Color.parseColor("#94A3B8")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("InkNote • $title • $dateStr", 28f, 32f, headerPaint)

        // 4. Draw Text Blocks
        for (tb in textBlocks) {
            val textPaint = Paint().apply {
                color = tb.colorHex.toInt()
                textSize = tb.fontSizeSp * 1.5f
                typeface = if (tb.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                isAntiAlias = true
            }

            val lines = tb.text.split("\n")
            var lineY = tb.y
            val lineHeight = textPaint.textSize * 1.3f

            for (line in lines) {
                canvas.drawText(line, tb.x, lineY, textPaint)
                lineY += lineHeight
            }
        }

        // 5. Draw Strokes with quadratic Bézier curve smoothing
        val strokePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        for (stroke in strokes) {
            if (stroke.points.size < 2) continue

            strokePaint.color = stroke.colorHex.toInt()
            strokePaint.strokeWidth = stroke.strokeWidth

            val path = android.graphics.Path()
            val points = stroke.points

            path.moveTo(points[0].x, points[0].y)

            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val midX = (prev.x + curr.x) / 2f
                val midY = (prev.y + curr.y) / 2f

                if (i == 1) {
                    path.lineTo(midX, midY)
                } else {
                    path.quadTo(prev.x, prev.y, midX, midY)
                }
            }
            val last = points.last()
            path.lineTo(last.x, last.y)

            canvas.drawPath(path, strokePaint)
        }

        // 6. Save as PNG
        val imgDir = File(context.cacheDir, "images").apply { mkdirs() }
        val safeFileName = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "InkNote" }
        val pngFile = File(imgDir, "${safeFileName}_${System.currentTimeMillis()}.png")

        if (outputStream != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        } else {
            FileOutputStream(pngFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
        }

        bitmap.recycle()
        pngFile
    }

    fun getFileUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun createSharePngIntent(context: Context, file: File, title: String): Intent {
        val uri = getFileUri(context, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Exported PNG sketch from InkNote on Vivo V23 5G")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
