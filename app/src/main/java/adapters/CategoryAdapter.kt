package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.R
import com.example.markettecnm.network.CategoryModel

/**
 * Adaptador para mostrar la lista de categorías.
 */
class CategoryAdapter(
    // CAMBIO 1 CLAVE: Cambiado de 'val' a 'var' para que la lista pueda ser actualizada
    private var categories: List<CategoryModel>,
    private val onItemClick: (CategoryModel) -> Unit // Función lambda para manejar el clic
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Asumiendo que el item_category.xml tiene un TextView para el nombre
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        // Puedes añadir aquí la ImageView si hay íconos de categoría
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        // Debes asegurar que este layout exista en res/layout/
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.tvCategoryName.text = category.name

        // Configurar el listener de clic
        holder.itemView.setOnClickListener {
            onItemClick(category)
        }

        // Si usas Glide/Picasso, cargarías la imagen de la categoría aquí si la tuvieras.
    }

    override fun getItemCount() = categories.size

    /**
     * CAMBIO 2 CLAVE: Función para actualizar la lista de categorías.
     * Esta es la función que CategoriasFragment busca y que faltaba.
     */
    fun updateCategories(newCategories: List<CategoryModel>) {
        this.categories = newCategories
        // Le dice al RecyclerView que los datos han cambiado y debe redibujarse
        notifyDataSetChanged()
    }
}