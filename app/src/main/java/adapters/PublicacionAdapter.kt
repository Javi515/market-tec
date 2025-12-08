package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.markettecnm.R
import com.example.markettecnm.models.ProductModel

class PublicacionAdapter(
    private var products: List<ProductModel>,
    // 🟢 Callback modificado: Pasa la ID de la acción (Int) y el Producto
    private val onProductAction: (Int, ProductModel) -> Unit
) : RecyclerView.Adapter<PublicacionAdapter.PublicacionViewHolder>() {

    inner class PublicacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        val tvProductStatus: TextView = itemView.findViewById(R.id.tvProductStatus)
        val btnOptions: ImageView = itemView.findViewById(R.id.btnOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicacionViewHolder {
        // Usamos R.layout.item_publicaciones tal como está en tu código
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_publicaciones, parent, false)
        return PublicacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PublicacionAdapter.PublicacionViewHolder, position: Int) {
        val product = products[position]

        holder.tvProductName.text = product.name
        holder.tvProductPrice.text = "$${product.price}"

        // Formatear estatus (Aplicando minúsculas/mayúsculas de forma segura)
        val statusText = product.status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
        }
        holder.tvProductStatus.text = "Estatus: $statusText"

        // Cargar imagen
        if (!product.image.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(product.image)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivProductImage)
        } else {
            holder.ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Lógica del Pop-up Menu de Opciones (Editar/Eliminar/Agotado)
        holder.btnOptions.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)

            // 🛑 Asegúrate de que el menú se llama menu_publicaciones.xml o menu_publicacion_options.xml
            // Usaremos R.menu.menu_publicaciones para consistencia con la respuesta anterior.
            popup.menuInflater.inflate(R.menu.menu_publicacion_options, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                // 🟢 Pasamos la ID del menú directamente al callback de la Activity
                onProductAction(item.itemId, product)
                true
            }
            popup.show()
        }

        // Clic en toda la fila para ver detalle
        holder.itemView.setOnClickListener {
            // 🟢 Usamos la ID de recurso como "acción" para mayor claridad
            onProductAction(R.id.action_view_product_detail, product)
        }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<ProductModel>) {
        this.products = newProducts
        notifyDataSetChanged()
    }
}