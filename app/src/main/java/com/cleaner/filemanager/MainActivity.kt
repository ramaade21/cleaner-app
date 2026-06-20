package com.cleaner.filemanager

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cleaner.filemanager.adapter.CategoryAdapter
import com.cleaner.filemanager.databinding.ActivityMainBinding
import com.cleaner.filemanager.model.CategoryCard
import com.cleaner.filemanager.model.CategoryType
import com.cleaner.filemanager.model.FileCategory
import com.cleaner.filemanager.util.FileScanner
import com.cleaner.filemanager.util.FileUtils
import com.cleaner.filemanager.util.ScanRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var scanJob: Job? = null
    private val scanner = FileScanner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        binding.btnGrantPermission.setOnClickListener { requestStoragePermission() }
        binding.btnScan.setOnClickListener { checkPermissionThenScan() }
        binding.swipeRefresh.setOnRefreshListener {
            checkPermissionThenScan()
        }

        updatePermissionUi()

        // Jika sudah pernah scan sebelumnya (ScanRepository), tampilkan langsung
        if (ScanRepository.allFiles.isNotEmpty()) {
            renderSummary()
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUi()
    }

    private fun hasAllFilesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    private fun updatePermissionUi() {
        val granted = hasAllFilesPermission()
        binding.layoutPermission.visibility = if (granted) android.view.View.GONE else android.view.View.VISIBLE
        binding.btnScan.isEnabled = granted
        binding.btnScan.alpha = if (granted) 1f else 0.5f
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }

    private fun checkPermissionThenScan() {
        if (!hasAllFilesPermission()) {
            Toast.makeText(this, "Mohon berikan izin akses file terlebih dahulu", Toast.LENGTH_SHORT).show()
            requestStoragePermission()
            return
        }
        startScan()
    }

    private fun startScan() {
        scanJob?.cancel()
        binding.swipeRefresh.isRefreshing = true
        binding.tvScanProgress.text = getString(R.string.scanning)

        scanJob = CoroutineScope(Dispatchers.IO).launch {
            val root = Environment.getExternalStorageDirectory()
            val (files, summary) = scanner.scanAll(root) { path ->
                CoroutineScope(Dispatchers.Main).launch {
                    binding.tvScanProgress.text = path
                }
            }

            ScanRepository.allFiles = files
            ScanRepository.summary = summary
            ScanRepository.lastScanTime = System.currentTimeMillis()

            withContext(Dispatchers.Main) {
                binding.swipeRefresh.isRefreshing = false
                binding.tvScanProgress.text = ""
                renderSummary()
            }
        }
    }

    private fun renderSummary() {
        val summary = ScanRepository.summary

        val used = summary.totalDeviceStorage - summary.freeStorage
        binding.tvUsedStorage.text = "${FileUtils.formatSize(used)} / ${FileUtils.formatSize(summary.totalDeviceStorage)}"
        binding.tvStorageDetail.text = "Tersisa ${FileUtils.formatSize(summary.freeStorage)}"

        val percent = if (summary.totalDeviceStorage > 0) {
            ((used.toDouble() / summary.totalDeviceStorage.toDouble()) * 100).toInt()
        } else 0
        binding.progressStorage.progress = percent.coerceIn(0, 100)

        val largeCount = ScanRepository.allFiles.count {
            !it.isDirectory && it.size >= FileScanner.LARGE_FILE_THRESHOLD
        }
        val largeSize = ScanRepository.allFiles
            .filter { !it.isDirectory && it.size >= FileScanner.LARGE_FILE_THRESHOLD }
            .sumOf { it.size }

        val cards = listOf(
            CategoryCard(CategoryType.HIDDEN, getString(R.string.hidden_files), summary.hiddenFilesSize, summary.hiddenFilesCount, R.drawable.ic_hidden),
            CategoryCard(CategoryType.CACHE, getString(R.string.cache_files), summary.cacheSize, summary.cacheCount, R.drawable.ic_cache),
            CategoryCard(CategoryType.LARGE, getString(R.string.large_files), largeSize, largeCount, R.drawable.ic_large),
            CategoryCard(CategoryType.IMAGE, getString(R.string.images), summary.imageSize, summary.imageCount, R.drawable.ic_image),
            CategoryCard(CategoryType.VIDEO, getString(R.string.videos), summary.videoSize, summary.videoCount, R.drawable.ic_video),
            CategoryCard(CategoryType.AUDIO, getString(R.string.audio_files), summary.audioSize, summary.audioCount, R.drawable.ic_file),
            CategoryCard(CategoryType.DOCUMENT, getString(R.string.documents), summary.documentSize, summary.documentCount, R.drawable.ic_document),
            CategoryCard(CategoryType.APK, getString(R.string.apk_files), summary.apkSize, summary.apkCount, R.drawable.ic_file),
            CategoryCard(CategoryType.APP_DATA, getString(R.string.app_data), summary.appDataSize, summary.appDataCount, R.drawable.ic_app_data),
            CategoryCard(CategoryType.OTHER, getString(R.string.others), summary.otherSize, summary.otherCount, R.drawable.ic_file)
        )

        binding.recyclerCategories.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerCategories.adapter = CategoryAdapter(cards) { card ->
            openFileList(card.type)
        }

        // Hitung selisih antara storage terpakai sesungguhnya vs total yang berhasil
        // dipindai & dikategorikan (hiddenFilesSize tidak ditambahkan karena nilainya
        // sudah termasuk di dalam kategori lain seperti image/video/dokumen, jadi akan
        // dobel jika dijumlahkan lagi di sini). Selisihnya biasanya folder yang dibatasi
        // sistem (misal sebagian Android/data milik app tertentu yang tetap terkunci
        // meski "All Files Access" sudah aktif). Ditampilkan agar tidak menyesatkan pengguna.
        val totalCategorized = summary.cacheSize + summary.imageSize + summary.videoSize +
            summary.audioSize + summary.documentSize + summary.apkSize +
            summary.appDataSize + summary.otherSize
        val unaccounted = used - totalCategorized

        if (unaccounted > FileScanner.LARGE_FILE_THRESHOLD) {
            binding.tvScanProgress.text = "Catatan: ${FileUtils.formatSize(unaccounted)} terpakai oleh sistem/app yang tidak bisa dipindai detail (folder dibatasi Android)"
        } else {
            binding.tvScanProgress.text = ""
        }
    }

    private fun openFileList(type: CategoryType) {
        if (ScanRepository.allFiles.isEmpty()) {
            Toast.makeText(this, "Silakan lakukan pemindaian terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, FileListActivity::class.java)
        intent.putExtra(FileListActivity.EXTRA_CATEGORY_TYPE, type.name)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        scanJob?.cancel()
        scanner.cancel()
    }
}
