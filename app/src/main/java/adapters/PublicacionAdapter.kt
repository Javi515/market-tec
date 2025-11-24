package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.markettecnm.R
import com.example.markettecnm.models.ProductModel

class PublicacionAdapter(
    private var products: List<ProductModel>,
    // Callback: action = "edit" o "delete", product = el item clickeado
    private val onProductAction: (String, ProductModel) -> Unit
) : RecyclerView.Adapter<PublicacionAdapter.PublicacionViewHolder>() {

    inner class PublicacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        val tvProductStatus: TextView = itemView.findViewById(R.id.tvProductStatus)
        val btnOptions: ImageView = itemView.findViewById(R.id.btnOptions) // Nuevo botón
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_publicaciones, parent, false)
        return PublicacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PublicacionAdapter.PublicacionViewHolder, position: Int) {
        val product = products[position]

        holder.tvProductName.text = product.name
        holder.tvProductPrice.text = "$${product.price}"

        // Formatear estatus (Pending, Active, etc.)
        holder.tvProductStatus.text = "Estatus: ${product.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"

        // Cargar imagen
        if (!product.image.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(product.image)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivProductImage)
        } else {
            holder.ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Lógica del Pop-up Menu de Opciones
        holder.btnOptions.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_publicacion_options, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit_product -> {
                        onProductAction("edit", product)
                        true
                    }
                    R.id.action_delete_product -> {
                        onProductAction("delete", product)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        // Click en toda la fila (opcional)
        holder.itemView.setOnClickListener {
            // Si quieres que al tocar la fila se abra el detalle, usa onProductAction("view", product)
            Toast.makeText(holder.itemView.context, "Ver detalle de ${product.name}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<ProductModel>) {
        this.products = newProducts
        notifyDataSetChanged()
    }
}