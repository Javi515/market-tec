package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.markettecnm.R
import com.example.markettecnm.network.ProductModel

/**
 * Adaptador para el ViewPager2 de Tendencias de la Semana.
 * Muestra productos como banners.
 */
class BannerAdapter(
    private val products: List<ProductModel>,
    private val onItemClick: (ProductModel) -> Unit
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    inner class BannerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivBannerImage: ImageView = view.findViewById(R.id.ivBannerImage)
        val tvBannerTitle: TextView = view.findViewById(R.id.tvBannerTitle)
        val tvBannerPrice: TextView = view.findViewById(R.id.tvBannerPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val product = products[position]

        holder.tvBannerTitle.text = product.name
        holder.tvBannerPrice.text = "¡Solo $${product.price}!"

        // Carga la imagen real o muestra el icono de carga
        val imageUrl = product.productImage
        if (!imageUrl.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.stat_notify_sync)  // ← Icono de carga giratorio
                .error(android.R.drawable.stat_notify_error)       // ← Icono si falla la descarga
                .into(holder.ivBannerImage)
        } else {
            // Si no hay imagen en el producto, ponemos una foto genérica o el mismo icono de carga
            holder.ivBannerImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Click en todo el banner
        holder.itemView.setOnClickListener {
            onItemClick(product)
        }
    }

    override fun getItemCount() = products.size
}