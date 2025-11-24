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

        // LOG PARA DEPURAR: Si no ves esto en el Logcat, la lista está vacía.
        Log.d("BANNER_DEBUG", "Cargando banner: ${product.name} - Img: ${product.image}")

        holder.tvBannerTitle.text = product.name
        holder.tvBannerPrice.text = "¡Solo $${product.price}!"

        val imageUrl = product.image

        if (!imageUrl.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .centerCrop()
                // Usamos ic_menu_gallery porque es gris oscuro y se nota más
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error) // Icono rojo si falla
                .into(holder.ivBannerImage)
        } else {
            // Si no hay URL, forzamos una imagen visible
            Log.d("BANNER_DEBUG", "Producto sin imagen, usando default")
            holder.ivBannerImage.setImageResource(android.R.drawable.ic_menu_gallery)
            // Opcional: Cambiar el color de fondo para que se note que hay algo
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
}