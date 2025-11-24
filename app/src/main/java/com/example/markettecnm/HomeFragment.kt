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

// Modelos correctos
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
                val deferredCategories = async { RetrofitClient.instance.getCategories() }
                val deferredProducts = async { RetrofitClient.instance.getProducts() }
                val deferredReviews = async { RetrofitClient.instance.getReviews() }

                val resCategories = deferredCategories.await()
                val resProducts = deferredProducts.await()
                val resReviews = deferredReviews.await()

                withContext(Dispatchers.Main) {
                    // A. Categorías
                    if (resCategories.isSuccessful) {
                        categoryAdapter.updateCategories(resCategories.body() ?: emptyList())
                    }

                    // B. Productos
                    val allProducts = resProducts.body() ?: emptyList()
                    if (resProducts.isSuccessful) {
                        productAdapter.updateProducts(allProducts)
                    }

                    // C. LÓGICA DE TENDENCIAS
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

        // 1. Agrupar reviews por ID de producto
        val reviewsByProduct = reviews.groupBy { it.product }

        // 2. Calcular promedio
        val productRatings = reviewsByProduct.mapValues { entry ->
            val totalStars = entry.value.sumOf { it.rating }
            val count = entry.value.size
            if (count > 0) totalStars.toDouble() / count else 0.0
        }

        // 3. Obtener los IDs de los 3 mejores
        val top3Ids = productRatings.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        Log.d("TENDENCIAS", "IDs ganadores: $top3Ids")

        // 4. Filtrar los productos ganadores
        // CORRECCIÓN AQUÍ: Quitamos el filtro de imagen (!it.image.isNullOrBlank())
        // Ahora el banner mostrará el producto aunque no tenga foto.
        val topProducts = products.filter {
            it.id in top3Ids
        }

        Log.d("TENDENCIAS", "Productos enviados al banner: ${topProducts.size}")

        // 5. Actualizar el BannerAdapter
        bannerAdapter.updateBanners(topProducts)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}