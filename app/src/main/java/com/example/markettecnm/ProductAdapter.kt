package com.example.markettecnm.adapters

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
import com.example.markettecnm.network.AddFavoriteRequest
import com.example.markettecnm.network.ProductModel
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductAdapter(
    private val products: List<ProductModel>,
    private val onItemClick: (ProductModel) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val favoriteCache = mutableSetOf<Int>()

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProduct: ImageView = itemView.findViewById(R.id.ivProduct)
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
        val isFavorite = favoriteCache.contains(product.id)

        holder.tvProductName.text = product.name
        holder.tvProductPrice.text = "$${product.price}"

        if (!product.productImage.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(product.productImage)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .centerCrop()
                .into(holder.ivProduct)
        } else {
            holder.ivProduct.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Asignación Inicial Robusta
        holder.btnFavorite.setImageResource(
            if (isFavorite) R.drawable.ic_favorite
            else R.drawable.ic_favorite_border
        )
        holder.btnFavorite.isSelected = isFavorite


        holder.btnFavorite.setOnClickListener {
            val wasFavorite = favoriteCache.contains(product.id)
            val isNowFavoriteOptimistic = !wasFavorite

            // Patrón Optimista: Cambia la UI inmediatamente
            holder.btnFavorite.setImageResource(
                if (isNowFavoriteOptimistic) R.drawable.ic_favorite
                else R.drawable.ic_favorite_border
            )
            holder.btnFavorite.isSelected = isNowFavoriteOptimistic

            holder.btnFavorite.scaleX = 0.8f
            holder.btnFavorite.scaleY = 0.8f
            holder.btnFavorite.animate().scaleX(1f).scaleY(1f).setDuration(200).start()

            CoroutineScope(Dispatchers.Main).launch {
                val success = toggleFavoriteOnServer(product.id, isNowFavoriteOptimistic)

                if (success) {
                    // ÉXITO: Actualizamos el caché
                    if (isNowFavoriteOptimistic) {
                        favoriteCache.add(product.id)
                    } else {
                        favoriteCache.remove(product.id)
                    }
                } else {
                    // FALLO: Revertimos el estado visual y notificamos
                    holder.btnFavorite.setImageResource(
                        if (wasFavorite) R.drawable.ic_favorite
                        else R.drawable.ic_favorite_border
                    )
                    holder.btnFavorite.isSelected = wasFavorite

                    Toast.makeText(
                        holder.itemView.context,
                        "Error de red o API. Verifica la URL base.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        holder.itemView.setOnClickListener { onItemClick(product) }
    }

    override fun getItemCount() = products.size

    private suspend fun toggleFavoriteOnServer(productId: Int, add: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            if (add) {
                // AÑADIR (POST /api/favorites/)
                val request = AddFavoriteRequest(product = productId)
                val response = RetrofitClient.instance.addFavorite(request)
                return@withContext response.isSuccessful

            } else {
                // REMOVER (GET + DELETE /api/favorites/{id}/)
                val getResponse = RetrofitClient.instance.getFavorites()

                if (getResponse.isSuccessful && getResponse.body() != null) {
                    // Buscar el ID del objeto favorito a eliminar
                    getResponse.body()!!.find { it.product.id == productId }?.id?.let { favoriteItemId ->

                        // Eliminar usando el ID del objeto favorito
                        val deleteResponse = RetrofitClient.instance.removeFavorite(favoriteItemId)
                        return@withContext deleteResponse.isSuccessful
                    } ?: run {
                        // El producto no se encontró en la lista para eliminar.
                        false
                    }
                } else {
                    // Falló la obtención de la lista de favoritos.
                    false
                }
            }
        } catch (e: Exception) {
            // Error de conexión (generalmente aquí es donde se lanza la excepción de Cleartext, si no está configurada)
            false
        }
    }

    fun preloadFavorites(favorites: List<ProductModel>) {
        favoriteCache.clear()
        favoriteCache.addAll(favorites.map { it.id })
        notifyDataSetChanged()
    }
}