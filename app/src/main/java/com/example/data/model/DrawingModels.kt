package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class PointData(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

@JsonClass(generateAdapter = true)
data class DrawingStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<PointData> = emptyList(),
    val colorHex: Long = 0xFF111827L,
    val strokeWidth: Float = 4f,
    val isEraser: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TextBlock(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val x: Float = 60f,
    val y: Float = 100f,
    val fontSizeSp: Float = 18f,
    val colorHex: Long = 0xFF111827L,
    val isBold: Boolean = false
)

enum class PaperPattern {
    BLANK,
    RULED,
    GRID,
    DOTS
}

enum class PaperTheme(
    val title: String,
    val isDark: Boolean,
    val backgroundHex: Long,
    val gridLineHex: Long,
    val defaultTextInk: Long
) {
    LIGHT_CLEAN("Clean White", false, 0xFFFFFFFFL, 0x1E000000L, 0xFF111827L),
    LIGHT_CREAM("Warm Cream", false, 0xFFFFFBEBL, 0x2292400EL, 0xFF111827L),
    LIGHT_RULED("Notebook Ruled", false, 0xFFF8FAFCL, 0x333B82F6L, 0xFF111827L),
    DARK_SLATE("Midnight Slate", true, 0xFF0F172AL, 0x2594A3B8L, 0xFFFFFFFFL),
    DARK_AMOLED("Pitch Black", true, 0xFF000000L, 0x22475569L, 0xFFFFFFFFL),
    DARK_GRID("Architect Dark", true, 0xFF18181BL, 0x2838BDF8L, 0xFFFFFFFFL)
}

object InkColors {
    const val BLACK_HEX: Long = 0xFF111827L
    const val BLUE_HEX: Long = 0xFF2563EBL
    const val RED_HEX: Long = 0xFFDC2626L
    const val WHITE_HEX: Long = 0xFFFFFFFFL

    val ALL = listOf(
        InkColorOption("Black", BLACK_HEX, 0xFF111827L),
        InkColorOption("Blue", BLUE_HEX, 0xFF2563EBL),
        InkColorOption("Red", RED_HEX, 0xFFDC2626L),
        InkColorOption("White", WHITE_HEX, 0xFFFFFFFFL)
    )
}

data class InkColorOption(
    val name: String,
    val hexValue: Long,
    val previewColor: Long
)

enum class ActiveTool {
    PEN,
    ERASER,
    TEXT
}

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val previewSnippet: String = "",
    val strokesJson: String = "[]",
    val textBlocksJson: String = "[]",
    val paperThemeName: String = PaperTheme.LIGHT_CLEAN.name,
    val paperPatternName: String = PaperPattern.RULED.name,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class NoteBackupItem(
    val id: Long,
    val title: String,
    val previewSnippet: String,
    val strokesJson: String,
    val textBlocksJson: String,
    val paperThemeName: String,
    val paperPatternName: String,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@JsonClass(generateAdapter = true)
data class BackupContainer(
    val app: String = "InkNote",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val notes: List<NoteBackupItem> = emptyList()
)
