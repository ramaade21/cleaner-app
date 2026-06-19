package com.cleaner.filemanager.util

import com.cleaner.filemanager.model.FileItem
import com.cleaner.filemanager.model.ScanSummary

/**
 * Penyimpanan sementara in-memory untuk hasil scan,
 * supaya tidak perlu scan ulang setiap pindah activity/screen.
 */
object ScanRepository {
    var allFiles: List<FileItem> = emptyList()
    var summary: ScanSummary = ScanSummary()
    var lastScanTime: Long = 0L

    fun clear() {
        allFiles = emptyList()
        summary = ScanSummary()
        lastScanTime = 0L
    }
}
