package com.cleaner.filemanager.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cleaner.filemanager.R
import com.cleaner.filemanager.model.FileCategory
import com.cleaner.filemanager.model.FileItem
import com.cleaner.filemanager.util.FileUtils

class FileListAdapter(
    private val items: MutableList<FileItem>,
    private val onSelectionChanged: () -> Unit,
    private val onItemClick: (FileItem) -> Unit,
    private val showCheckbox: Boolean = true
) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkItem: CheckBox = view.findViewById(R.id.checkItem)
        val imgThumb: ImageView = view.findViewById(R.id.imgThumb)
        val tvName: TextView = view.findViewById(R.id.tvFileName)
        val tvPath: TextView = view.findViewById(R.id.tvFilePath)
        val tvSize: TextView = view.findViewById(R.id.tvFileSize)
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvPath.text = item.path
        holder.tvSize.text = FileUtils.formatSize(item.size)

        // Set listener ke null dulu sebelum set checked, supaya tidak memicu callback lama saat recycle
        holder.checkItem.setOnCheckedChangeListener(null)
        holder.checkItem.isChecked = item.isSelected
        holder.checkItem.visibility = if (showCheckbox) View.VISIBLE else View.GONE
        holder.checkItem.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
            onSelectionChanged()
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        // Thumbnail: gunakan Glide untuk gambar/video, icon statis untuk lainnya
        when (item.category) {
            FileCategory.IMAGE -> {
                Glide.with(holder.imgThumb.context)
                    .load(item.file)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_image)
                    .into(holder.imgThumb)
            }
            FileCategory.VIDEO -> {
                Glide.with(holder.imgThumb.context)
                    .load(item.file)
                    .centerCrop()
                    .placeholder(R.drawable.ic_video)
                    .error(R.drawable.ic_video)
                    .into(holder.imgThumb)
            }
            FileCategory.DOCUMENT -> holder.imgThumb.setImageResource(R.drawable.ic_document)
            FileCategory.CACHE -> holder.imgThumb.setImageResource(R.drawable.ic_cache)
            FileCategory.APP_DATA -> holder.imgThumb.setImageResource(R.drawable.ic_app_data)
            else -> holder.imgThumb.setImageResource(R.drawable.ic_file)
        }
    }

    override fun getItemCount(): Int = items.size

    fun setAllSelected(selected: Boolean) {
        for (item in items) item.isSelected = selected
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<FileItem> = items.filter { it.isSelected }

    fun toggleSelection(item: FileItem) {
        val index = items.indexOf(item)
        if (index != -1) {
            items[index].isSelected = !items[index].isSelected
            notifyItemChanged(index)
            onSelectionChanged()
        }
    }

    fun removeItems(removed: List<FileItem>) {
        items.removeAll(removed.toSet())
        notifyDataSetChanged()
    }
}
