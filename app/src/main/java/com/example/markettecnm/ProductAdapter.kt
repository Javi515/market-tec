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

    // Conjunto de IDs que son favoritos
    private val favoriteCache = mutableSetOf<Int>()

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProduct: ImageView = itemView.findViewById(R.id.ivProductImage)
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        // 1. Determinar si es favorito según el caché local
        val isFavorite = favoriteCache.contains(product.id)

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

        // 2. Pintar el corazón correctamente al cargar
        updateFavoriteIcon(holder.btnFavorite, isFavorite)

        // 3. LÓGICA DE CLIC (TOGGLE)
        holder.btnFavorite.setOnClickListener {
            // Estado actual ANTES de hacer el cambio
            val wasFavorite = favoriteCache.contains(product.id)

            // Estado deseado (lo opuesto al actual)
            val newStatus = !wasFavorite

            // A. Cambio Visual Inmediato (Optimista)
            updateFavoriteIcon(holder.btnFavorite, newStatus)

            // Animación pequeña
            holder.btnFavorite.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction {
                holder.btnFavorite.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()

            // B. Actualizar caché local temporalmente
            if (newStatus) {
                favoriteCache.add(product.id)
                Toast.makeText(holder.itemView.context, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
            } else {
                favoriteCache.remove(product.id)
                Toast.makeText(holder.itemView.context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
            }

            // C. Llamada al Servidor en segundo plano
            CoroutineScope(Dispatchers.Main).launch {
                val success = toggleFavoriteOnServer(product.id)

                if (!success) {
                    // D. SI FALLA: Revertimos todo (Rollback)
                    Log.e("FAV_ERROR", "El servidor falló al cambiar favorito ID: ${product.id}")

                    if (wasFavorite) favoriteCache.add(product.id) else favoriteCache.remove(product.id)
                    updateFavoriteIcon(holder.btnFavorite, wasFavorite) // Regresamos al icono anterior

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
        notifyDataSetChanged()
    }

    fun preloadFavorites(favoriteIds: List<Int>) {
        favoriteCache.clear()
        favoriteCache.addAll(favoriteIds)
        notifyDataSetChanged()
    }

    private fun updateFavoriteIcon(button: ImageButton, isFavorite: Boolean) {
        button.setImageResource(
            if (isFavorite) R.drawable.ic_favorite // Relleno
            else R.drawable.ic_favorite_border   // Borde
        )
    }

    private suspend fun toggleFavoriteOnServer(productId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("API_CALL", "Enviando Toggle para producto ID: $productId")
            val request = ToggleFavoriteRequest(productId)
            val response = RetrofitClient.instance.toggleFavorite(request)

            if (!response.isSuccessful) {
                // IMPRIMIR EL ERROR EXACTO DEL SERVIDOR
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