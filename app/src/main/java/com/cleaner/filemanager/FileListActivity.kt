package com.cleaner.filemanager

import android.app.AlertDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.cleaner.filemanager.adapter.FileListAdapter
import com.cleaner.filemanager.databinding.ActivityFileListBinding
import com.cleaner.filemanager.model.CategoryType
import com.cleaner.filemanager.model.FileCategory
import com.cleaner.filemanager.model.FileItem
import com.cleaner.filemanager.util.FileScanner
import com.cleaner.filemanager.util.FileUtils
import com.cleaner.filemanager.util.ScanRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY_TYPE = "extra_category_type"
    }

    private lateinit var binding: ActivityFileListBinding
    private lateinit var adapter: FileListAdapter
    private var currentList: MutableList<FileItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val typeStr = intent.getStringExtra(EXTRA_CATEGORY_TYPE) ?: CategoryType.OTHER.name
        val type = CategoryType.valueOf(typeStr)
        binding.toolbar.title = titleForType(type)

        loadFiles(type)

        binding.checkSelectAll.setOnCheckedChangeListener { _, isChecked ->
            adapter.setAllSelected(isChecked)
            updateSelectionInfo()
        }

        binding.btnDeleteSelected.setOnClickListener {
            confirmDelete()
        }
    }

    private fun titleForType(type: CategoryType): String = when (type) {
        CategoryType.HIDDEN -> getString(R.string.hidden_files)
        CategoryType.CACHE -> getString(R.string.cache_files)
        CategoryType.LARGE -> getString(R.string.large_files)
        CategoryType.IMAGE -> getString(R.string.images)
        CategoryType.VIDEO -> getString(R.string.videos)
        CategoryType.AUDIO -> getString(R.string.audio_files)
        CategoryType.DOCUMENT -> getString(R.string.documents)
        CategoryType.APK -> getString(R.string.apk_files)
        CategoryType.APP_DATA -> getString(R.string.app_data)
        CategoryType.OTHER -> getString(R.string.others)
    }

    private fun loadFiles(type: CategoryType) {
        binding.progressLoading.visibility = android.view.View.VISIBLE
        binding.recyclerFiles.visibility = android.view.View.GONE

        // Data Aplikasi (Android/data, Android/obb) sengaja tidak bisa dihapus dari
        // sini karena berisiko merusak app lain yang masih terinstall. Untuk membersihkan
        // data app tertentu, arahkan pengguna ke Settings > Apps > [nama app] > Clear Data.
        val isDeletable = type != CategoryType.APP_DATA
        binding.checkSelectAll.visibility = if (isDeletable) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnDeleteSelected.visibility = if (isDeletable) android.view.View.VISIBLE else android.view.View.GONE

        if (!isDeletable) {
            Toast.makeText(
                this,
                "Data aplikasi tidak bisa dihapus di sini. Untuk membersihkan, buka Settings > Apps > [nama app] > Hapus Cache/Data.",
                Toast.LENGTH_LONG
            ).show()
        }

        CoroutineScope(Dispatchers.Default).launch {
            val all = ScanRepository.allFiles

            val filtered = when (type) {
                CategoryType.HIDDEN -> all.filter { it.isHidden && !it.isDirectory }
                CategoryType.CACHE -> all.filter { it.category == FileCategory.CACHE }
                CategoryType.LARGE -> all.filter { !it.isDirectory && it.size >= FileScanner.LARGE_FILE_THRESHOLD }
                CategoryType.IMAGE -> all.filter { it.category == FileCategory.IMAGE }
                CategoryType.VIDEO -> all.filter { it.category == FileCategory.VIDEO }
                CategoryType.AUDIO -> all.filter { it.category == FileCategory.AUDIO }
                CategoryType.DOCUMENT -> all.filter { it.category == FileCategory.DOCUMENT }
                CategoryType.APK -> all.filter { it.category == FileCategory.APK }
                CategoryType.APP_DATA -> all.filter { it.category == FileCategory.APP_DATA }
                CategoryType.OTHER -> all.filter { it.category == FileCategory.OTHER && !it.isDirectory }
            }.sortedByDescending { it.size }.toMutableList()

            withContext(Dispatchers.Main) {
                currentList = filtered
                binding.progressLoading.visibility = android.view.View.GONE

                if (currentList.isEmpty()) {
                    binding.tvEmpty.visibility = android.view.View.VISIBLE
                    binding.recyclerFiles.visibility = android.view.View.GONE
                } else {
                    binding.tvEmpty.visibility = android.view.View.GONE
                    binding.recyclerFiles.visibility = android.view.View.VISIBLE
                }

                adapter = FileListAdapter(
                    items = currentList,
                    onSelectionChanged = { updateSelectionInfo() },
                    onItemClick = { item -> showPreviewDialog(item) },
                    showCheckbox = isDeletable
                )
                binding.recyclerFiles.layoutManager = LinearLayoutManager(this@FileListActivity)
                binding.recyclerFiles.adapter = adapter
                updateSelectionInfo()
            }
        }
    }

    private fun showPreviewDialog(item: FileItem) {
        val dateStr = DateFormat.format("dd MMM yyyy, HH:mm", item.lastModified)
        val message = buildString {
            append("Path: ${item.path}\n")
            append("Ukuran: ${FileUtils.formatSize(item.size)}\n")
            append("Diubah: $dateStr")
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(item.name)
            .setMessage(message)
            .setPositiveButton("Tutup", null)

        // Tampilkan thumbnail preview untuk gambar/video langsung di dialog
        if (item.category == FileCategory.IMAGE || item.category == FileCategory.VIDEO) {
            val imageView = ImageView(this).apply {
                val heightPx = (220 * resources.displayMetrics.density).toInt()
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    heightPx
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(0, 16, 0, 16)
            }
            Glide.with(this)
                .load(item.file)
                .centerCrop()
                .into(imageView)

            val container = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(48, 16, 48, 0)
                addView(imageView)
            }
            builder.setView(container)
        }

        // Tombol hapus langsung dari dialog preview (kecuali untuk Data Aplikasi)
        if (item.category != FileCategory.APP_DATA) {
            builder.setNegativeButton("Hapus") { _, _ ->
                confirmDeleteSingle(item)
            }
        }

        builder.show()
    }

    private fun confirmDeleteSingle(item: FileItem) {
        AlertDialog.Builder(this)
            .setTitle("Hapus file ini?")
            .setMessage("${item.name} (${FileUtils.formatSize(item.size)}) akan dihapus permanen.")
            .setPositiveButton("Hapus") { _, _ -> performDelete(listOf(item)) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateSelectionInfo() {
        val selected = adapter.getSelectedItems()
        val totalSize = selected.sumOf { it.size }
        binding.tvSelectedInfo.text = "${selected.size} dipilih · ${FileUtils.formatSize(totalSize)}"

        val hasSelection = selected.isNotEmpty()
        binding.btnDeleteSelected.isEnabled = hasSelection
        binding.btnDeleteSelected.alpha = if (hasSelection) 1f else 0.5f
    }

    private fun confirmDelete() {
        val selected = adapter.getSelectedItems()
        if (selected.isEmpty()) return

        val totalSize = selected.sumOf { it.size }
        AlertDialog.Builder(this)
            .setTitle("Hapus ${selected.size} file?")
            .setMessage("File yang dihapus tidak dapat dikembalikan. Total ukuran: ${FileUtils.formatSize(totalSize)}")
            .setPositiveButton("Hapus") { _, _ -> performDelete(selected) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performDelete(selected: List<FileItem>) {
        CoroutineScope(Dispatchers.IO).launch {
            val deleted = mutableListOf<FileItem>()
            var freedSize = 0L

            for (item in selected) {
                val success = if (item.isDirectory) {
                    item.file.deleteRecursively()
                } else {
                    item.file.delete()
                }
                if (success) {
                    deleted.add(item)
                    freedSize += item.size
                }
            }

            // Update repository global agar konsisten di dashboard
            ScanRepository.allFiles = ScanRepository.allFiles.toMutableList().apply {
                removeAll(deleted.toSet())
            }

            withContext(Dispatchers.Main) {
                adapter.removeItems(deleted)
                updateSelectionInfo()
                binding.checkSelectAll.isChecked = false

                if (currentList.isEmpty()) {
                    binding.tvEmpty.visibility = android.view.View.VISIBLE
                    binding.recyclerFiles.visibility = android.view.View.GONE
                }

                Toast.makeText(
                    this@FileListActivity,
                    "${deleted.size} file dihapus, ${FileUtils.formatSize(freedSize)} dibebaskan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
