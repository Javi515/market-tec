package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Necesario para imágenes de la API
import com.example.markettecnm.R
import com.example.markettecnm.models.ProductModel // Usar el modelo real

class ComprasAdapter(
    // CAMBIO 1: Usar el modelo real de la API
    private val comprasList: List<ProductModel>,
    private val purchaseQuantities: Map<String, Int>, // Mapa de ID -> Cantidad
    private val onContactClick: (ProductModel) -> Unit
) : RecyclerView.Adapter<ComprasAdapter.CompraViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompraViewHolder {
        // Asegúrate de tener este layout creado: item_compra.xml
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_compra, parent, false)
        return CompraViewHolder(view)
    }

    override fun onBindViewHolder(holder: CompraViewHolder, position: Int) {
        val product = comprasList[position]
        // Obtener la cantidad guardada en las preferencias
        val quantity = purchaseQuantities[product.id.toString()] ?: 1
        holder.bind(product, quantity)
    }

    override fun getItemCount() = comprasList.size

    inner class CompraViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProducto: ImageView = itemView.findViewById(R.id.ivCompraProducto)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvCompraNombre)
        private val tvCantidad: TextView = itemView.findViewById(R.id.tvCompraCantidad)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvCompraEstado)
        private val btnContactar: Button = itemView.findViewById(R.id.btnContactarVendedor)

        // CAMBIO 2: Ahora recibe ProductModel y la Cantidad
        fun bind(product: ProductModel, quantity: Int) {

            // Cargar imagen con Glide
            if (!product.image.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(product.image)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivProducto)
            } else {
                ivProducto.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            tvNombre.text = product.name
            // CAMBIO 3: Usar la cantidad real de la compra
            tvCantidad.text = "Cantidad: $quantity"

            // Usamos un valor fijo, ya que el estado no está en el modelo
            tvEstado.text = "Estado: Entregado"

            btnContactar.setOnClickListener {
                onContactClick(product)
                Toast.makeText(itemView.context, "Iniciando chat con vendedor de: ${product.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}