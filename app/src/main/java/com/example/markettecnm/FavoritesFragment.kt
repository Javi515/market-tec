package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ProductAdapter
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Importamos los modelos correctos
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.network.FavoriteResponse

class FavoritesFragment : Fragment() {

    private lateinit var rvFavorites: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)
        rvFavorites = view.findViewById(R.id.rvFavorites)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvFavorites.layoutManager = LinearLayoutManager(requireContext())
        rvFavorites.isNestedScrollingEnabled = false
    }

    // Al volver a la pantalla, recargamos los favoritos
    override fun onResume() {
        super.onResume()
        loadFavoritesFromServer()
    }

    private fun loadFavoritesFromServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getFavorites()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        // Mapeamos de FavoriteResponse a ProductModel
                        val favoriteProducts = response.body()!!.map { it.product }

                        if (favoriteProducts.isEmpty()) {
                            showEmptyState()
                        } else {
                            showFavorites(favoriteProducts)
                        }
                    } else {
                        // Manejo básico de error de API (401, 404, etc.)
                        showEmptyState()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("Favorites", "Error de conexión", e)
                    showEmptyState()
                }
            }
        }
    }

    private fun showFavorites(products: List<ProductModel>) {
        tvEmpty.visibility = View.GONE
        rvFavorites.visibility = View.VISIBLE

        // 1. Inicializamos el adaptador
        val adapter = ProductAdapter(
            products = products,
            onItemClick = { product ->
                val intent = Intent(requireContext(), ProductDetailActivity::class.java).apply {
                    putExtra("product_id", product.id)
                }
                startActivity(intent)
            }
        )

        // 2. PRECARGA EL CACHÉ: Obtenemos los IDs de la lista actual (todos son favoritos)
        val favoriteIds = products.map { it.id }
        adapter.preloadFavorites(favoriteIds) // Forzamos al adapter a pintar los corazones llenos (rojos)

        // 3. Establecemos el adaptador
        rvFavorites.adapter = adapter
    }

    private fun showEmptyState() {
        tvEmpty.visibility = View.VISIBLE
        rvFavorites.visibility = View.GONE
        tvEmpty.text = "No tienes productos favoritos aún ❤️"
    }
}