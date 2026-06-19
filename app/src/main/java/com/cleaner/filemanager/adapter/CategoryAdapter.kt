package com.cleaner.filemanager.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cleaner.filemanager.R
import com.cleaner.filemanager.model.CategoryCard
import com.cleaner.filemanager.util.FileUtils

class CategoryAdapter(
    private val items: List<CategoryCard>,
    private val onClick: (CategoryCard) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: android.widget.ImageView = view.findViewById(R.id.imgIcon)
        val tvName: android.widget.TextView = view.findViewById(R.id.tvCategoryName)
        val tvSize: android.widget.TextView = view.findViewById(R.id.tvCategorySize)
        val tvCount: android.widget.TextView = view.findViewById(R.id.tvCategoryCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.title
        holder.tvSize.text = FileUtils.formatSize(item.size)
        holder.tvCount.text = "${item.count} item"
        holder.imgIcon.setImageResource(item.iconRes)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
