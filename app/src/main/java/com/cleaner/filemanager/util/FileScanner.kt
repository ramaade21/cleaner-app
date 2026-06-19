package com.cleaner.filemanager.util

import android.os.Environment
import com.cleaner.filemanager.model.FileCategory
import com.cleaner.filemanager.model.FileItem
import com.cleaner.filemanager.model.ScanSummary
import java.io.File

/**
 * Melakukan scanning rekursif ke seluruh penyimpanan eksternal.
 * Dipanggil dari background thread (lihat MainActivity/FileListActivity).
 */
class FileScanner {

    companion object {
        // 50 MB dianggap "file besar"
        const val LARGE_FILE_THRESHOLD = 50L * 1024 * 1024

        // Folder yang dilewati agar scan tidak terlalu lama / tidak relevan
        private val SKIP_DIRS = setOf(
            "Android/obb"
        )
    }

    @Volatile
    var isCancelled = false

    /**
     * Scan seluruh root storage, mengumpulkan semua FileItem beserta ringkasannya.
     * onProgress dipanggil sesekali untuk update UI (path yang sedang discan).
     */
    fun scanAll(
        root: File,
        onProgress: (String) -> Unit = {}
    ): Pair<List<FileItem>, ScanSummary> {
        val result = mutableListOf<FileItem>()
        val summary = ScanSummary()

        val stat = android.os.StatFs(root.path)
        summary.totalDeviceStorage = stat.totalBytes
        summary.freeStorage = stat.availableBytes

        scanRecursive(root, result, summary, onProgress)
        return Pair(result, summary)
    }

    private fun scanRecursive(
        dir: File,
        result: MutableList<FileItem>,
        summary: ScanSummary,
        onProgress: (String) -> Unit
    ) {
        if (isCancelled) return
        if (!dir.canRead()) return

        val relativePath = dir.path
        for (skip in SKIP_DIRS) {
            if (relativePath.contains(skip)) return
        }

        val children = dir.listFiles() ?: return
        onProgress(dir.path)

        for (child in children) {
            if (isCancelled) return

            val isHidden = FileUtils.isHiddenPath(child)
            val isCacheDir = child.isDirectory && child.name.equals("cache", ignoreCase = true)

            if (child.isDirectory) {
                if (isCacheDir) {
                    // Hitung sebagai satu entri cache (ukuran folder, bukan rekursif file individu)
                    val size = FileUtils.calculateDirSize(child)
                    summary.cacheSize += size
                    summary.cacheCount += 1
                    result.add(
                        FileItem(
                            file = child,
                            name = child.name,
                            path = child.path,
                            size = size,
                            lastModified = child.lastModified(),
                            isDirectory = true,
                            isHidden = isHidden,
                            category = FileCategory.CACHE
                        )
                    )
                    // tidak masuk lebih dalam ke folder cache
                    continue
                } else {
                    scanRecursive(child, result, summary, onProgress)
                    continue
                }
            }

            // File biasa
            val size = child.length()
            val category = FileUtils.categorize(child)
            summary.totalScanned += size

            if (isHidden) {
                summary.hiddenFilesSize += size
                summary.hiddenFilesCount += 1
            }

            when (category) {
                FileCategory.IMAGE -> { summary.imageSize += size; summary.imageCount++ }
                FileCategory.VIDEO -> { summary.videoSize += size; summary.videoCount++ }
                FileCategory.AUDIO -> { summary.audioSize += size; summary.audioCount++ }
                FileCategory.DOCUMENT -> { summary.documentSize += size; summary.documentCount++ }
                FileCategory.APK -> { summary.apkSize += size; summary.apkCount++ }
                else -> { summary.otherSize += size; summary.otherCount++ }
            }

            result.add(
                FileItem(
                    file = child,
                    name = child.name,
                    path = child.path,
                    size = size,
                    lastModified = child.lastModified(),
                    isDirectory = false,
                    isHidden = isHidden,
                    category = category
                )
            )
        }
    }

    fun cancel() {
        isCancelled = true
    }
}
