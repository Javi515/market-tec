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
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 👇 CORRECCIÓN 1: Importar el modelo correcto
import com.example.markettecnm.models.ProductModel

class ProductAdapter(
    // 👇 CORRECCIÓN 2: 'var' para poder actualizar la lista desde HomeFragment
    private var products: List<ProductModel>,
    private val onItemClick: (ProductModel) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val favoriteCache = mutableSetOf<Int>()

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Asegúrate de que estos IDs coincidan con tu item_product.xml
        val ivProduct: ImageView = itemView.findViewById(R.id.ivProductImage) // A veces lo tienes como ivProductImage o ivProduct
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice) // A veces lo tienes como tvPrice
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

        // 👇 CORRECCIÓN 3: Usar 'image' (definido en models.kt), no 'productImage'
        if (!product.image.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(product.image)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .centerCrop()
                .into(holder.ivProduct)
        } else {
            holder.ivProduct.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Estado visual del botón favorito
        holder.btnFavorite.setImageResource(
            if (isFavorite) R.drawable.ic_favorite
            else R.drawable.ic_favorite_border
        )
        holder.btnFavorite.isSelected = isFavorite

        // Click en Favorito
        holder.btnFavorite.setOnClickListener {
            val wasFavorite = favoriteCache.contains(product.id)
            val isNowFavoriteOptimistic = !wasFavorite

            // Animación y cambio visual inmediato (Optimista)
            holder.btnFavorite.setImageResource(
                if (isNowFavoriteOptimistic) R.drawable.ic_favorite
                else R.drawable.ic_favorite_border
            )
            holder.btnFavorite.scaleX = 0.8f
            holder.btnFavorite.scaleY = 0.8f
            holder.btnFavorite.animate().scaleX(1f).scaleY(1f).setDuration(200).start()

            CoroutineScope(Dispatchers.Main).launch {
                val success = toggleFavoriteOnServer(product.id, isNowFavoriteOptimistic)

                if (success) {
                    if (isNowFavoriteOptimistic) favoriteCache.add(product.id)
                    else favoriteCache.remove(product.id)
                } else {
                    // Revertir si falla
                    holder.btnFavorite.setImageResource(
                        if (wasFavorite) R.drawable.ic_favorite
                        else R.drawable.ic_favorite_border
                    )
                    Toast.makeText(holder.itemView.context, "Error al actualizar favorito", Toast.LENGTH_SHORT).show()
                }
            }
        }

        holder.itemView.setOnClickListener { onItemClick(product) }
    }

    override fun getItemCount() = products.size

    // 👇 CORRECCIÓN 4: Función necesaria para el HomeFragment
    fun updateProducts(newProducts: List<ProductModel>) {
        this.products = newProducts
        notifyDataSetChanged()
    }

    private suspend fun toggleFavoriteOnServer(productId: Int, add: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            if (add) {
                // 👇 CORRECCIÓN 5: Usar el nombre de parámetro correcto (productId)
                val request = AddFavoriteRequest(productId = productId)
                val response = RetrofitClient.instance.addFavorite(request)
                return@withContext response.isSuccessful
            } else {
                // Lógica para borrar favorito (Buscar ID -> Borrar)
                val getResponse = RetrofitClient.instance.getFavorites()
                if (getResponse.isSuccessful && getResponse.body() != null) {
                    getResponse.body()!!.find { it.product.id == productId }?.id?.let { favId ->
                        val deleteResponse = RetrofitClient.instance.removeFavorite(favId)
                        return@withContext deleteResponse.isSuccessful
                    } ?: false
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}