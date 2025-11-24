package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Importante para las imágenes
import com.example.markettecnm.R
import com.example.markettecnm.network.CategoryModel

/**
 * Adaptador para mostrar la lista de categorías con imágenes desde Internet.
 */
class CategoryAdapter(
    private var categories: List<CategoryModel>,
    private val onItemClick: (CategoryModel) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        val ivCategoryImage: ImageView = view.findViewById(R.id.ivCategoryImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        // 1. Asignar nombre
        holder.tvCategoryName.text = category.name

        // 2. Cargar imagen usando Glide
        // NOTA: 'category.image' debe ser la URL que viene de tu JSON
        Glide.with(holder.itemView.context)
            .load(category.image)
            .placeholder(R.drawable.ic_launcher_background) // Muestra esto mientras carga
            .error(R.drawable.ic_placeholder_product)       // Muestra esto si falla la carga
            .into(holder.ivCategoryImage)

        // 3. Configurar clic
        holder.itemView.setOnClickListener {
            onItemClick(category)
        }
    }

    override fun getItemCount() = categories.size

    // Función para actualizar datos desde el Fragmento
    fun updateCategories(newCategories: List<CategoryModel>) {
        this.categories = newCategories
        notifyDataSetChanged()
    }
}