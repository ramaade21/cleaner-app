package com.cleaner.filemanager.util

import com.cleaner.filemanager.model.FileCategory
import java.io.File
import java.text.DecimalFormat
import java.util.Locale

object FileUtils {

    private val imageExt = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "svg")
    private val videoExt = setOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "flv", "wmv")
    private val audioExt = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a", "wma")
    private val documentExt = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf")
    private val archiveExt = setOf("zip", "rar", "7z", "tar", "gz")

    /**
     * Format ukuran byte menjadi string yang mudah dibaca (KB, MB, GB).
     */
    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val safeGroup = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, safeGroup.toDouble())
        val df = DecimalFormat("#,##0.##")
        return "${df.format(value)} ${units[safeGroup]}"
    }

    /**
     * Menentukan kategori file berdasarkan ekstensi dan lokasi path.
     */
    fun categorize(file: File): FileCategory {
        val name = file.name.lowercase(Locale.ROOT)
        val ext = name.substringAfterLast('.', "")
        val path = file.path.lowercase(Locale.ROOT)

        return when {
            path.contains("/cache/") || path.contains(".cache") || name == "cache" -> FileCategory.CACHE
            ext == "apk" -> FileCategory.APK
            ext in imageExt -> FileCategory.IMAGE
            ext in videoExt -> FileCategory.VIDEO
            ext in audioExt -> FileCategory.AUDIO
            ext in documentExt -> FileCategory.DOCUMENT
            ext in archiveExt -> FileCategory.ARCHIVE
            else -> FileCategory.OTHER
        }
    }

    /**
     * Cek apakah file/folder dianggap tersembunyi (nama diawali titik,
     * atau berada di dalam folder yang diawali titik).
     */
    fun isHiddenPath(file: File): Boolean {
        if (file.isHidden) return true
        var current: File? = file
        while (current != null) {
            if (current.name.startsWith(".")) return true
            current = current.parentFile
        }
        return false
    }

    /**
     * Hitung total ukuran sebuah folder secara rekursif (dengan limit kedalaman
     * untuk menghindari proses terlalu lama).
     */
    fun calculateDirSize(dir: File, maxDepth: Int = 12, currentDepth: Int = 0): Long {
        if (currentDepth > maxDepth) return 0L
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) {
                calculateDirSize(f, maxDepth, currentDepth + 1)
            } else {
                f.length()
            }
        }
        return size
    }
}
