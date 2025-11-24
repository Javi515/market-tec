package com.example.markettecnm.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.markettecnm.R
import com.example.markettecnm.models.ProductModel

class BannerAdapter(
    private var products: List<ProductModel>,
    private val onItemClick: (ProductModel) -> Unit
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    // 💡 NUEVO: Mapa para almacenar ProductID -> Average Rating
    private var ratingMap: Map<Int, Double> = emptyMap()

    inner class BannerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivBannerImage: ImageView = view.findViewById(R.id.ivBannerImage)
        val tvBannerTitle: TextView = view.findViewById(R.id.tvBannerTitle)
        val tvBannerPrice: TextView = view.findViewById(R.id.tvBannerPrice)
        // 🛠️ ASUMIDO: Si tu XML tiene un TextView para el rating en el banner
        // val tvBannerRating: TextView = view.findViewById(R.id.tvBannerRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val product = products[position]

        // 1. Obtener Rating y Actualizar UI
        val averageRating = ratingMap[product.id]
        if (averageRating != null && averageRating > 0) {
            // Si el layout tiene tvBannerRating:
            // holder.tvBannerRating.text = String.format("⭐ %.1f", averageRating)
            // holder.tvBannerRating.visibility = View.VISIBLE

            // Si el layout NO tiene un TextView dedicado para rating,
            // y solo queremos mostrarlo en Logcat, ignoramos este paso visual.
        }


        // LOG PARA DEPURAR:
        Log.d("BANNER_DEBUG", "Cargando banner: ${product.name} - Rating: ${averageRating ?: 0.0}")

        holder.tvBannerTitle.text = product.name
        holder.tvBannerPrice.text = "¡Solo $${product.price}!"

        val imageUrl = product.image

        // Carga de imagen
        if (!imageUrl.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .into(holder.ivBannerImage)
        } else {
            Log.d("BANNER_DEBUG", "Producto sin imagen, usando default")
            holder.ivBannerImage.setImageResource(android.R.drawable.ic_menu_gallery)
            holder.ivBannerImage.setBackgroundColor(android.graphics.Color.LTGRAY)
        }

        holder.itemView.setOnClickListener {
            onItemClick(product)
        }
    }

    override fun getItemCount(): Int {
        Log.d("BANNER_DEBUG", "Total items en banner: ${products.size}")
        return products.size
    }

    fun updateBanners(newProducts: List<ProductModel>) {
        Log.d("BANNER_DEBUG", "Actualizando banners con ${newProducts.size} productos")
        this.products = newProducts
        notifyDataSetChanged()
    }

    // 🛑 FUNCIÓN FALTANTE: Permite a HomeFragment enviar el mapa de calificaciones
    fun updateRatings(newRatingMap: Map<Int, Double>) {
        this.ratingMap = newRatingMap
        // Nota: NO llamamos notifyDataSetChanged() aquí para evitar doble redraw.
        // Lo hará updateBanners() justo después.
    }
}