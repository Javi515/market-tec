package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.R
import com.example.markettecnm.models.Product

class ComprasAdapter(
    private val comprasList: List<Product>,
    private val onContactClick: (Product) -> Unit
) : RecyclerView.Adapter<ComprasAdapter.CompraViewHolder>() {

    /**
     * Crea la vista de la fila (infla el layout item_compra.xml)
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompraViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_compra, parent, false)
        return CompraViewHolder(view)
    }

    /**
     * Conecta los datos (producto) con la vista (holder)
     */
    override fun onBindViewHolder(holder: CompraViewHolder, position: Int) {
        val product = comprasList[position]
        holder.bind(product)
    }

    /**
     * Devuelve el número total de items en la lista
     */
    override fun getItemCount() = comprasList.size

    /**
     * Clase interna que controla la vista de cada fila
     */
    inner class CompraViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Enlaza las vistas del layout item_compra.xml
        private val ivProducto: ImageView = itemView.findViewById(R.id.ivCompraProducto)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvCompraNombre)
        private val tvCantidad: TextView = itemView.findViewById(R.id.tvCompraCantidad)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvCompraEstado)
        private val btnContactar: Button = itemView.findViewById(R.id.btnContactarVendedor)

        // Función para "rellenar" la vista con los datos del producto
        fun bind(product: Product) {
            ivProducto.setImageResource(product.imageRes)
            tvNombre.text = product.name
            tvCantidad.text = "Cantidad: ${product.quantityInCart}"

            // Por ahora, el estado es fijo, pero podrías guardarlo en el futuro
            tvEstado.text = "Estado: En envío"

            // Configura el clic del botón de contacto
            btnContactar.setOnClickListener {
                onContactClick(product)

                // Muestra un mensaje temporal al hacer clic
                Toast.makeText(itemView.context, "Iniciando chat con vendedor de: ${product.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}