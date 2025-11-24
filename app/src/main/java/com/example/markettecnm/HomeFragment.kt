package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.markettecnm.adapters.BannerAdapter
import com.example.markettecnm.adapters.CategoryAdapter
import com.example.markettecnm.adapters.ProductAdapter
import com.example.markettecnm.databinding.FragmentHomeBinding
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.models.ReviewModel
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var productAdapter: ProductAdapter
    private lateinit var bannerAdapter: BannerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        fetchData()
    }

    private fun setupRecyclerViews() {
        // 1. TENDENCIAS (CARRUSEL)
        bannerAdapter = BannerAdapter(emptyList()) { product ->
            val intent = Intent(requireContext(), ProductDetailActivity::class.java)
            intent.putExtra("product_id", product.id)
            startActivity(intent)
        }
        binding.vpTendenciaCarousel.adapter = bannerAdapter

        // 2. CATEGORÍAS
        categoryAdapter = CategoryAdapter(emptyList()) { category ->
            Log.d("HomeFragment", "Click en categoría: ${category.name}")
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        // 3. LISTA DE PRODUCTOS
        productAdapter = ProductAdapter(emptyList()) { product ->
            val intent = Intent(requireContext(), ProductDetailActivity::class.java)
            intent.putExtra("product_id", product.id)
            startActivity(intent)
        }
        binding.rvProducts.apply {
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = productAdapter
        }
    }

    private fun fetchData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // CARGA EN PARALELO: Categorías, Productos, Reviews y FAVORITOS
                val deferredCategories = async { RetrofitClient.instance.getCategories() }
                val deferredProducts = async { RetrofitClient.instance.getProducts() }
                val deferredReviews = async { RetrofitClient.instance.getReviews() }
                val deferredFavorites = async { RetrofitClient.instance.getFavorites() } // <--- NUEVO

                val resCategories = deferredCategories.await()
                val resProducts = deferredProducts.await()
                val resReviews = deferredReviews.await()
                val resFavorites = deferredFavorites.await() // <--- NUEVO

                withContext(Dispatchers.Main) {
                    // A. Categorías
                    if (resCategories.isSuccessful) {
                        categoryAdapter.updateCategories(resCategories.body() ?: emptyList())
                    }

                    // B. Favoritos (Pre-carga para pintar corazones rojos)
                    val favoriteIds = if (resFavorites.isSuccessful && resFavorites.body() != null) {
                        // Obtenemos solo los IDs de los productos favoritos
                        resFavorites.body()!!.map { it.product.id }
                    } else {
                        emptyList()
                    }
                    // Le avisamos al adaptador cuáles son favoritos antes de cargar los productos
                    // NOTA: Necesitaremos agregar esta función 'preloadFavorites' al ProductAdapter en el siguiente paso
                    productAdapter.preloadFavorites(favoriteIds)

                    // C. Productos
                    val allProducts = resProducts.body() ?: emptyList()
                    if (resProducts.isSuccessful) {
                        productAdapter.updateProducts(allProducts)
                    }

                    // D. Tendencias
                    if (resReviews.isSuccessful && resProducts.isSuccessful) {
                        val allReviews = resReviews.body() ?: emptyList()
                        Log.d("TENDENCIAS", "Reviews descargadas: ${allReviews.size}")
                        calculateTrends(allProducts, allReviews)
                    } else {
                        Log.e("TENDENCIAS", "Error cargando reviews o productos")
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error cargando datos", e)
            }
        }
    }

    private fun calculateTrends(products: List<ProductModel>, reviews: List<ReviewModel>) {
        if (reviews.isEmpty()) {
            Log.d("TENDENCIAS", "La lista de reviews está vacía. No hay tendencias.")
            return
        }

        val reviewsByProduct = reviews.groupBy { it.product }

        val productRatings = reviewsByProduct.mapValues { entry ->
            val totalStars = entry.value.sumOf { it.rating }
            val count = entry.value.size
            if (count > 0) totalStars.toDouble() / count else 0.0
        }

        val top3Ids = productRatings.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        Log.d("TENDENCIAS", "IDs ganadores: $top3Ids")

        val topProducts = products.filter {
            it.id in top3Ids
        }

        Log.d("TENDENCIAS", "Productos enviados al banner: ${topProducts.size}")

        bannerAdapter.updateBanners(topProducts)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}