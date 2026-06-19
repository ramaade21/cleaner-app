package com.cleaner.filemanager

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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
        CategoryType.OTHER -> getString(R.string.others)
    }

    private fun loadFiles(type: CategoryType) {
        binding.progressLoading.visibility = android.view.View.VISIBLE
        binding.recyclerFiles.visibility = android.view.View.GONE

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

                adapter = FileListAdapter(currentList) { updateSelectionInfo() }
                binding.recyclerFiles.layoutManager = LinearLayoutManager(this@FileListActivity)
                binding.recyclerFiles.adapter = adapter
                updateSelectionInfo()
            }
        }
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
