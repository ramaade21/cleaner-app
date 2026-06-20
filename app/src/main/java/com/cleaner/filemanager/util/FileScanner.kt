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

        // Folder data internal aplikasi lain (game assets, cache app, dll).
        // Tidak di-scan rekursif (isinya ribuan file kecil tidak relevan untuk ditampilkan
        // satu-satu), TAPI ukurannya tetap dihitung total dan ditampilkan sebagai kartu
        // "Data Aplikasi" — supaya tidak hilang begitu saja dari total penyimpanan.
        private val APP_DATA_DIR_NAMES = setOf("data", "obb")

        // Folder thumbnail/cache sistem yang memang tidak perlu ditampilkan detail,
        // tapi tetap dihitung sebagai bagian dari "Cache" (bukan dibuang dari total).
        private val SYSTEM_THUMBNAIL_DIRS = setOf(".thumbnails", ".trash", ".cache")

        // Update progress UI paling cepat setiap 150ms, bukan setiap folder,
        // supaya tidak membebani main thread dan scan terasa lebih cepat & lancar.
        private const val PROGRESS_THROTTLE_MS = 150L
    }

    @Volatile
    var isCancelled = false

    private var lastProgressUpdate = 0L

    /**
     * Scan seluruh root storage, mengumpulkan semua FileItem beserta ringkasannya.
     * onProgress dipanggil sesekali (di-throttle) untuk update UI (path yang sedang discan).
     */
    fun scanAll(
        root: File,
        onProgress: (String) -> Unit = {}
    ): Pair<List<FileItem>, ScanSummary> {
        val result = mutableListOf<FileItem>()
        val summary = ScanSummary()
        lastProgressUpdate = 0L

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

        if (!dir.canRead()) {
            // Folder ada tapi tidak bisa dibaca (dibatasi sistem Android, biasanya
            // folder data milik app lain meski izin "All Files Access" sudah aktif).
            // Catat ukurannya jika memungkinkan (StatFs tidak bisa per-folder, jadi
            // kita hanya bisa menandai keberadaannya, bukan ukuran pastinya).
            return
        }

        val children = dir.listFiles()
        if (children == null) {
            // listFiles() mengembalikan null biasanya berarti permission ditolak
            // walau canRead() sempat true (race condition khas Android storage).
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastProgressUpdate >= PROGRESS_THROTTLE_MS) {
            lastProgressUpdate = now
            onProgress(dir.path)
        }

        for (child in children) {
            if (isCancelled) return

            val isHidden = FileUtils.isHiddenPath(child)
            val isCacheDir = child.isDirectory && child.name.equals("cache", ignoreCase = true)
            val isAppDataDir = child.isDirectory &&
                dir.name.equals("Android", ignoreCase = true) &&
                child.name.lowercase() in APP_DATA_DIR_NAMES

            if (child.isDirectory) {
                if (isAppDataDir) {
                    // Folder Android/data atau Android/obb: hitung total ukurannya
                    // (mencakup data semua app terinstall) tapi jangan rinci per file,
                    // supaya scan tetap cepat dan hasilnya tetap akurat di total storage.
                    val size = FileUtils.calculateDirSize(child)
                    summary.appDataSize += size
                    summary.appDataCount += 1
                    result.add(
                        FileItem(
                            file = child,
                            name = "Data Aplikasi (${child.name})",
                            path = child.path,
                            size = size,
                            lastModified = child.lastModified(),
                            isDirectory = true,
                            isHidden = isHidden,
                            category = FileCategory.APP_DATA
                        )
                    )
                    continue
                }
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
