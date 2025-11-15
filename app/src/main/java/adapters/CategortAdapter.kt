package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.R
import com.example.markettecnm.models.Category

class CategoryAdapter(
    private val categories: List<Category>,
    private val layoutResId: Int, // ← permite usar distintos layouts
    private val onClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCategoryImage: ImageView = itemView.findViewById(R.id.ivCategoryImage)
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.tvCategoryName.text = category.name

        val imageResId = when (category.name.lowercase()) {
            "electrónica" -> R.drawable.xbox
            "moda" -> R.drawable.moda
            "hogar" -> R.drawable.hogar
            "deportes" -> R.drawable.deportes
            "libros" -> R.drawable.libros
            else -> R.drawable.xbox
        }

        holder.ivCategoryImage.setImageResource(imageResId)

        holder.itemView.setOnClickListener {
            onClick(category)
        }
    }

    override fun getItemCount(): Int = categories.size
}