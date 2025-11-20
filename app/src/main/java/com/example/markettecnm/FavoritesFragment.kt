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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ProductAdapter
import com.example.markettecnm.network.FavoriteResponse  // ← ¡IMPORT CLAVE!
import com.example.markettecnm.network.ProductModel
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                        // ← MAPPING PERFECTO: del JSON → FavoriteResponse → ProductModel
                        val favoriteProducts = response.body()!!.map { it.product }

                        if (favoriteProducts.isEmpty()) {
                            showEmptyState()
                        } else {
                            showFavorites(favoriteProducts)
                        }
                    } else {
                        Toast.makeText(requireContext(), "Error al cargar favoritos", Toast.LENGTH_SHORT).show()
                        showEmptyState()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("Favorites", "Error de conexión", e)
                    Toast.makeText(requireContext(), "Sin conexión", Toast.LENGTH_SHORT).show()
                    showEmptyState()
                }
            }
        }
    }

    private fun showFavorites(products: List<ProductModel>) {
        tvEmpty.visibility = View.GONE
        rvFavorites.visibility = View.VISIBLE

        rvFavorites.adapter = ProductAdapter(
            products = products,
            onItemClick = { product ->
                val intent = Intent(requireContext(), ProductDetailActivity::class.java).apply {
                    putExtra("PRODUCT_ID", product.id)
                }
                startActivity(intent)
            }
        )
    }

    private fun showEmptyState() {
        tvEmpty.visibility = View.VISIBLE
        rvFavorites.visibility = View.GONE
        tvEmpty.text = "No tienes productos favoritos aún ❤️"
    }

    override fun onResume() {
        super.onResume()
        loadFavoritesFromServer()  // Recarga cada vez que entras
    }
}