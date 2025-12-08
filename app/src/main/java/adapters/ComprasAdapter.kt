package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat // 🟢 Importación necesaria para obtener colores
import com.bumptech.glide.Glide
import com.example.markettecnm.R
import com.example.markettecnm.models.OrderResponse
import com.example.markettecnm.models.ProductModel
import java.util.Locale

class ComprasAdapter(
    // Recibir la lista de órdenes completas
    private var ordersList: List<OrderResponse>,

    // Callbacks para la actividad (Contactar y Cancelar)
    private val onContactClick: (productId: Int, productName: String) -> Unit,
    private val onCancelClick: (orderId: Int) -> Unit
) : RecyclerView.Adapter<ComprasAdapter.CompraViewHolder>() {

    // FUNCIÓN DE ACTUALIZACIÓN: Necesaria para recargar tras una cancelación o carga inicial
    fun updateData(newOrdersList: List<OrderResponse>) {
        ordersList = newOrdersList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompraViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_compra, parent, false)
        return CompraViewHolder(view)
    }

    override fun onBindViewHolder(holder: CompraViewHolder, position: Int) {
        val order = ordersList[position]
        holder.bind(order)
    }

    override fun getItemCount() = ordersList.size

    inner class CompraViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Inicialización de Vistas (IDs verificados en item_compra.xml)
        private val ivProducto: ImageView = itemView.findViewById(R.id.ivCompraProducto)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvCompraNombre)
        private val tvCantidad: TextView = itemView.findViewById(R.id.tvCompraCantidad)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvCompraEstado)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvCompraTotal)
        private val btnContactar: Button = itemView.findViewById(R.id.btnContactarVendedor)
        private val btnCancelar: Button = itemView.findViewById(R.id.btnCancelarCompra)

        fun bind(order: OrderResponse) {
            val firstItem = order.items.firstOrNull()
            val product = firstItem?.product
            val quantity = firstItem?.quantity ?: 0
            val statusKey = order.status

            // 1. CARGAR IMAGEN Y NOMBRE DEL PRODUCTO
            if (product != null) {
                tvNombre.text = product.name
                tvCantidad.text = "Cantidad: $quantity"

                if (!product.image.isNullOrEmpty()) {
                    Glide.with(itemView.context)
                        .load(product.image)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(ivProducto)
                } else {
                    ivProducto.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } else {
                tvNombre.text = "Orden sin productos"
                tvCantidad.text = "Cantidad: 0"
                ivProducto.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // 2. ESTADO Y TOTAL
            tvTotal.text = "Total: $${order.totalPrice}"

            // 🟢 Aplicar traducción y estilo de color al TextView del estado
            tvEstado.text = translateOrderStatus(statusKey)
            applyStatusStyle(tvEstado, statusKey)

            // 3. CONTACTAR VENDEDOR
            btnContactar.setOnClickListener {
                if (product != null) {
                    onContactClick(product.id, product.name)
                }
            }

            // 4. LÓGICA DE CANCELACIÓN: Solo se puede cancelar si el estado lo permite
            val currentStatus = statusKey.lowercase(Locale.ROOT)
            val canCancel = currentStatus == "pending_payment" || currentStatus == "processing"

            if (canCancel) {
                btnCancelar.visibility = View.VISIBLE
                btnCancelar.isEnabled = true
                btnCancelar.setOnClickListener {
                    onCancelClick(order.id)
                }
            } else {
                // Si ya está enviada, entregada o cancelada, ocultar el botón
                btnCancelar.visibility = View.GONE
                btnCancelar.isEnabled = false
            }
        }
    }

    // 🟢 FUNCIÓN DE ESTILO DE ESTADO: Aplica color y fondo
    private fun applyStatusStyle(textView: TextView, status: String) {
        val context = textView.context
        val normalizedStatus = status.lowercase(Locale.ROOT)

        // Colores predeterminados (para estados intermedios o desconocidos)
        var bgColor = R.drawable.bg_status_red
        var textColor = ContextCompat.getColor(context, R.color.status_yellow_text)

        when (normalizedStatus) {
            // VERDE: Entregado
            "delivered" -> {
                bgColor = R.drawable.bg_status_green
                textColor = ContextCompat.getColor(context, R.color.status_green_text)
            }
            // ROJO: Cancelado
            "cancelled" -> {
                bgColor = R.drawable.bg_status_red
                textColor = ContextCompat.getColor(context, R.color.status_red_text)
            }
            // AMARILLO/NARANJA: Pendiente, En Proceso, Enviado
            "pending_payment", "processing", "shipped" -> {
                bgColor = R.drawable.bg_status_yellow
                textColor = ContextCompat.getColor(context, R.color.status_yellow_text)
            }
        }

        textView.setBackgroundResource(bgColor)
        textView.setTextColor(textColor)
    }

    // FUNCIÓN DE TRADUCCIÓN: Versión estable que utiliza Locale.ROOT para comparación segura
    private fun translateOrderStatus(status: String): String {
        // Normalizamos el estado a minúsculas para asegurar la coincidencia
        val normalizedStatus = status.lowercase(Locale.ROOT)

        val translatedStatus = when (normalizedStatus) {
            "pending_payment" -> "Pago Pendiente"
            "processing" -> "En Proceso"
            "shipped" -> "Enviado"
            "delivered" -> "Entregado"
            "cancelled" -> "Cancelado"
            else -> status.replace('_', ' ')
        }

        // Reemplaza la primera letra con mayúscula de forma segura
        return translatedStatus.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}