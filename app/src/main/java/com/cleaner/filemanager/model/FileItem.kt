package com.cleaner.filemanager.model

import java.io.File

/**
 * Merepresentasikan satu file/folder yang ditemukan saat scanning.
 */
data class FileItem(
    val file: File,
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val isHidden: Boolean,
    val category: FileCategory,
    var isSelected: Boolean = false
)

enum class FileCategory {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    APK,
    ARCHIVE,
    CACHE,
    OTHER
}

/**
 * Ringkasan hasil scan untuk ditampilkan di dashboard.
 */
data class ScanSummary(
    var hiddenFilesSize: Long = 0L,
    var hiddenFilesCount: Int = 0,
    var cacheSize: Long = 0L,
    var cacheCount: Int = 0,
    var imageSize: Long = 0L,
    var imageCount: Int = 0,
    var videoSize: Long = 0L,
    var videoCount: Int = 0,
    var documentSize: Long = 0L,
    var documentCount: Int = 0,
    var audioSize: Long = 0L,
    var audioCount: Int = 0,
    var apkSize: Long = 0L,
    var apkCount: Int = 0,
    var otherSize: Long = 0L,
    var otherCount: Int = 0,
    var totalScanned: Long = 0L,
    var totalDeviceStorage: Long = 0L,
    var freeStorage: Long = 0L
)
