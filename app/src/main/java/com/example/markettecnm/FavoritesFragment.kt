package com.example.markettecnm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ProductAdapter
import com.example.markettecnm.models.Product

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
        updateFavorites()
    }

    fun updateFavorites() {
        val sharedPrefs = requireContext().getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val favoriteIds = sharedPrefs.getStringSet("ids", setOf()) ?: setOf()

        val allProducts = listOf(
            Product(1, "Producto 1", 59.99, 4.0f, 24, R.drawable.papas_fritas),
            Product(2, "Teclado Mecánico", 85.00, 4.5f, 150, R.drawable.teclado),
            Product(3, "Camiseta Vintage", 25.50, 4.8f, 78, R.drawable.camiseta),
            Product(4, "Mouse Inalámbrico", 19.99, 4.2f, 95, R.drawable.mouse)
        )

        val favorites = allProducts.filter { favoriteIds.contains(it.id.toString()) }

        if (favorites.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvFavorites.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvFavorites.visibility = View.VISIBLE
            rvFavorites.adapter = ProductAdapter(
                favorites,
                onClick = { product ->
                    val intent = Intent(requireContext(), ProductDetailActivity::class.java).apply {
                        putExtra("product", product)
                    }
                    startActivity(intent)
                },
                context = requireContext(),
                onFavoriteChanged = {
                    updateFavorites()
                }
            )
        }
    }
}