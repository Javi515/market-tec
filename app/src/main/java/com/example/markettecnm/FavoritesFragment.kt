package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ProductAdapter
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 👇 CORRECCIÓN CLAVE: El import DEBE ser este (models), NO network
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

        loadFavoritesFromServer()
    }

    private fun loadFavoritesFromServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getFavorites()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        // Al importar ProductModel correctamente arriba, este map funcionará
                        val favoriteProducts = response.body()!!.map { it.product }

                        if (favoriteProducts.isEmpty()) {
                            showEmptyState()
                        } else {
                            showFavorites(favoriteProducts)
                        }
                    } else {
                        if (response.code() == 404) {
                            showEmptyState()
                        } else {
                            Log.e("Favs", "Error code: ${response.code()}")
                            showEmptyState()
                        }
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

    // Recibe List<ProductModel> (del paquete models)
    private fun showFavorites(list: List<ProductModel>) {
        tvEmpty.visibility = View.GONE
        rvFavorites.visibility = View.VISIBLE

        // CORRECCIÓN: Pasamos la lista directamente sin nombre de variable ("products =")
        // para evitar errores si tu adaptador usa otro nombre en el constructor.
        rvFavorites.adapter = ProductAdapter(list) { product ->

            // Aquí 'product' ya es reconocido correctamente
            val intent = Intent(requireContext(), ProductDetailActivity::class.java)
            // Error 'id' resuelto:
            intent.putExtra("product_id", product.id)
            startActivity(intent)
        }
    }

    private fun showEmptyState() {
        tvEmpty.visibility = View.VISIBLE
        rvFavorites.visibility = View.GONE
        tvEmpty.text = "No tienes productos favoritos aún ❤️"
    }

    override fun onResume() {
        super.onResume()
        loadFavoritesFromServer()
    }
}