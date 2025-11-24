package com.example.markettecnm.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.markettecnm.R
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.network.RetrofitClient
import com.example.markettecnm.network.ToggleFavoriteRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductAdapter(
    private var products: List<ProductModel>,
    private val onItemClick: (ProductModel) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val favoriteCache = mutableSetOf<Int>()
    private var ratingMap: Map<Int, Double> = emptyMap()

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProduct: ImageView = itemView.findViewById(R.id.ivProductImage)
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
        val tvProductRating: TextView = itemView.findViewById(R.id.tvProductRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        val isFavorite = favoriteCache.contains(product.id)

        // 🛑 LÓGICA CLAVE: Mostrar el rating real del mapa
        val averageRating = ratingMap[product.id]

        if (averageRating != null && averageRating > 0) {
            holder.tvProductRating.text = String.format("⭐ %.1f", averageRating)
            holder.tvProductRating.visibility = View.VISIBLE
        } else {
            holder.tvProductRating.visibility = View.GONE
        }

        holder.tvProductName.text = product.name
        holder.tvProductPrice.text = "$${product.price}"

        // Carga de imagen
        if (!product.image.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(product.image)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.ivProduct)
        } else {
            holder.ivProduct.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        updateFavoriteIcon(holder.btnFavorite, isFavorite)

        // LÓGICA DE CLIC (TOGGLE)
        holder.btnFavorite.setOnClickListener {
            val wasFavorite = favoriteCache.contains(product.id)
            val newStatus = !wasFavorite

            updateFavoriteIcon(holder.btnFavorite, newStatus)

            holder.btnFavorite.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction {
                holder.btnFavorite.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()

            if (newStatus) {
                favoriteCache.add(product.id)
                Toast.makeText(holder.itemView.context, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
            } else {
                favoriteCache.remove(product.id)
                Toast.makeText(holder.itemView.context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
            }

            CoroutineScope(Dispatchers.Main).launch {
                val success = toggleFavoriteOnServer(product.id)

                if (!success) {
                    Log.e("FAV_ERROR", "El servidor falló al cambiar favorito ID: ${product.id}")
                    if (wasFavorite) favoriteCache.add(product.id) else favoriteCache.remove(product.id)
                    updateFavoriteIcon(holder.btnFavorite, wasFavorite)
                    Toast.makeText(holder.itemView.context, "Error al sincronizar favorito", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("FAV_SUCCESS", "Favorito actualizado en servidor correctamente")
                }
            }
        }

        holder.itemView.setOnClickListener { onItemClick(product) }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<ProductModel>) {
        this.products = newProducts
        notifyDataSetChanged() // La actualización principal de datos debe notificar el cambio
    }

    fun preloadFavorites(favoriteIds: List<Int>) {
        // 🛠️ OPTIMIZACIÓN: Solo actualizamos la caché, no notificamos el cambio aquí.
        // HomeFragment llamará a updateProducts() justo después, que hará el redraw.
        favoriteCache.clear()
        favoriteCache.addAll(favoriteIds)
    }

    // 💡 FUNCIÓN FALTANTE: Para recibir el mapa de ratings desde HomeFragment
    fun updateRatings(newRatingMap: Map<Int, Double>) {
        this.ratingMap = newRatingMap
        notifyDataSetChanged() // Forzamos la actualización de las estrellas en pantalla
    }

    private fun updateFavoriteIcon(button: ImageButton, isFavorite: Boolean) {
        button.setImageResource(
            if (isFavorite) R.drawable.ic_favorite
            else R.drawable.ic_favorite_border
        )
    }

    private suspend fun toggleFavoriteOnServer(productId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("API_CALL", "Enviando Toggle para producto ID: $productId")
            val request = ToggleFavoriteRequest(productId)
            val response = RetrofitClient.instance.toggleFavorite(request)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Log.e("API_ERROR", "Código: ${response.code()} - Cuerpo: $errorBody")
            }

            return@withContext response.isSuccessful
        } catch (e: Exception) {
            Log.e("API_EXCEPTION", "Error de red: ${e.message}")
            return@withContext false
        }
    }
}