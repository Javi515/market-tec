package com.example.markettecnm.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.R
import com.example.markettecnm.models.Product

class ProductAdapter(
    private var products: List<Product>, // ← ahora mutable
    private val onClick: (Product) -> Unit,
    private val context: Context,
    private val onFavoriteChanged: (() -> Unit)? = null
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    private val sharedPrefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    private val favoriteIds by lazy {
        sharedPrefs.getStringSet("ids", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProduct: ImageView = itemView.findViewById(R.id.ivProduct)
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        val tvProductRating: TextView = itemView.findViewById(R.id.tvProductRating)
        val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]

        holder.ivProduct.setImageResource(product.imageRes)
        holder.tvProductName.text = product.name
        holder.tvProductPrice.text = "$${String.format("%.2f", product.price)}"

        // --- LÍNEA MODIFICADA ---
        // Se cambió product.reviewsCount por product.stock para que coincida con tu modelo Product.kt
        holder.tvProductRating.text = String.format("⭐ %.1f (%d)", product.rating, product.stock)

        val idStr = product.id.toString()
        val isFavorite = favoriteIds.contains(idStr)

        holder.btnFavorite.setImageResource(
            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )

        holder.btnFavorite.setOnClickListener {
            if (isFavorite) {
                favoriteIds.remove(idStr)
            } else {
                favoriteIds.add(idStr)
            }
            sharedPrefs.edit().putStringSet("ids", favoriteIds).apply()
            notifyItemChanged(position)
            onFavoriteChanged?.invoke()
        }

        holder.itemView.setOnClickListener { onClick(product) }
    }

    override fun getItemCount() = products.size

    // ✅ Método para actualizar la lista de productos
    fun updateProducts(newList: List<Product>) {
        products = newList
        notifyDataSetChanged()
    }
}