package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.markettecnm.R
import com.example.markettecnm.models.OrderResponse
import java.util.Locale

class VentasAdapter(
    private var sales: List<OrderResponse>,
    private val onContactClick: (Int, String) -> Unit // (ClientId, ProductName)
) : RecyclerView.Adapter<VentasAdapter.VentaViewHolder>() {

    class VentaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val ivProductImage: ImageView = view.findViewById(R.id.ivProductImage)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvClientInfo: TextView = view.findViewById(R.id.tvClientInfo)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
        val btnContact: Button = view.findViewById(R.id.btnContactClient)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VentaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_venta, parent, false)
        return VentaViewHolder(view)
    }

    override fun onBindViewHolder(holder: VentaViewHolder, position: Int) {
        val sale = sales[position]

        val firstItem = sale.items.firstOrNull()
        val productName = firstItem?.product?.name ?: "Producto desconocido"
        val productImg = firstItem?.product?.image

        val statusKey = sale.status

        holder.tvStatus.text = translateOrderStatus(statusKey)
        applyStatusStyle(holder.tvStatus, statusKey)

        holder.tvDate.text = sale.createdAt.take(10)

        holder.tvProductName.text = productName
        holder.tvTotal.text = "Total: $${sale.totalPrice}"

        // 🟢 CORRECCIÓN: Acceso seguro al nombre del comprador.
        // **ASUMIMOS** que has añadido una propiedad "clientUsername" a tu modelo OrderResponse.

        val clientUsername: String? = try {
            val field = sale::class.java.getDeclaredField("clientUsername")
            field.isAccessible = true
            field.get(sale) as? String
        } catch (e: NoSuchFieldException) {
            null
        } catch (e: Exception) {
            null
        }

        val clientDisplay = clientUsername?.takeIf { it.isNotBlank() } ?: "ID: ${sale.client}"
        holder.tvClientInfo.text = "Comprador: $clientDisplay"
        // -------------------------------------------------------------

        if (!productImg.isNullOrEmpty()) {
            Glide.with(holder.itemView.context).load(productImg).into(holder.ivProductImage)
        } else {
            holder.ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.btnContact.setOnClickListener {
            onContactClick(sale.client, productName)
        }
    }

    override fun getItemCount() = sales.size

    // FUNCIÓN DE ESTILO DE ESTADO: Aplica color y fondo
    private fun applyStatusStyle(textView: TextView, status: String) {
        val context = textView.context
        val normalizedStatus = status.lowercase(Locale.ROOT)

        var bgColor = R.drawable.bg_status_red
        var textColor = ContextCompat.getColor(context, R.color.status_yellow_text)

        when (normalizedStatus) {
            "delivered" -> {
                bgColor = R.drawable.bg_status_green
                textColor = ContextCompat.getColor(context, R.color.status_green_text)
            }
            "cancelled" -> {
                bgColor = R.drawable.bg_status_red
                textColor = ContextCompat.getColor(context, R.color.status_red_text)
            }
            "pending_payment", "processing", "shipped" -> {
                bgColor = R.drawable.bg_status_yellow
                textColor = ContextCompat.getColor(context, R.color.status_yellow_text)
            }
        }

        textView.setBackgroundResource(bgColor)
        textView.setTextColor(textColor)
    }

    // FUNCIÓN DE TRADUCCIÓN: Implementación estable y a prueba de mayúsculas/minúsculas
    private fun translateOrderStatus(status: String): String {

        val normalizedStatus = status
            .replace("-", "_")
            .lowercase(Locale.ROOT)

        val translatedStatus = when (normalizedStatus) {
            "pending_payment" -> "Pago Pendiente"
            "processing" -> "En Proceso"
            "shipped" -> "Enviado"
            "delivered" -> "Entregado"
            "cancelled" -> "Cancelado"
            else -> status.replace('_', ' ')
        }

        return translatedStatus.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}