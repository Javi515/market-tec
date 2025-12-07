package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.markettecnm.R

// 👇 CORRECCIÓN IMPORTANTE: Apuntar al paquete 'models' unificado
import com.example.markettecnm.models.CategoryModel

/**
 * Adaptador para mostrar la lista de categorías.
 * Si no hay imagen, muestra el icono estándar de galería.
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

        // 2. Lógica de Imagen: Priorizamos el icono de galería si algo falla
        if (!category.image.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(category.image)
                // Mientras carga, muestra el icono de galería
                .placeholder(android.R.drawable.ic_menu_gallery)
                // Si la carga falla (error de red), muestra el icono de galería
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.ivCategoryImage)
        } else {
            // Si el servidor mandó null o string vacío, ponemos el icono manualmente
            holder.ivCategoryImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // 3. Configurar clic
        holder.itemView.setOnClickListener {
            onItemClick(category)
        }
    }

    override fun getItemCount() = categories.size

    fun updateCategories(newCategories: List<CategoryModel>) {
        this.categories = newCategories
        notifyDataSetChanged()
    }
}