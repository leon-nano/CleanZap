package com.limpamidia.app.data

import android.net.Uri
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Tipos de mídia do WhatsApp, identificados pelo nome da pasta (ex.: "WhatsApp Voice Notes"). */
enum class MediaCategory(
    val label: String,
    val emoji: String,
    val folderKeyword: String,
    val visual: Boolean = false,
    val audio: Boolean = false,
) {
    VOICE("Mensagens de voz", "🎤", "Voice Notes", audio = true),
    AUDIO("Áudios", "🎵", "Audio", audio = true),
    IMAGES("Fotos", "🖼️", "Images", visual = true),
    VIDEO("Vídeos", "🎬", "Video", visual = true),
    GIFS("GIFs", "👾", "Animated Gifs", visual = true),
    DOCUMENTS("Documentos", "📄", "Documents");

    companion object {
        fun fromFolderName(name: String): MediaCategory? =
            entries.firstOrNull { name.contains(it.folderKeyword, ignoreCase = true) }
    }
}

enum class DateFilter(val label: String) {
    TODAY("Hoje"),
    YESTERDAY("Ontem"),
    WEEK("7 dias"),
    MONTH("30 dias"),
    ALL("Tudo");

    fun matches(date: LocalDate, today: LocalDate): Boolean = when (this) {
        TODAY -> date == today
        YESTERDAY -> date == today.minusDays(1)
        WEEK -> !date.isBefore(today.minusDays(6))
        MONTH -> !date.isBefore(today.minusDays(29))
        ALL -> true
    }
}

data class MediaItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val mime: String,
    val size: Long,
    val modified: Long,
    val date: LocalDate,
    val folder: String,
    val category: MediaCategory,
) {
    val sent: Boolean get() = folder.equals("Sent", ignoreCase = true)

    val sortKey: Long
        get() = if (modified > 0) modified
        else date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")
private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

fun MediaItem.whenLabel(): String =
    if (modified > 0) Instant.ofEpochMilli(modified).atZone(ZoneId.systemDefault()).format(timeFormat)
    else date.format(dayFormat)
