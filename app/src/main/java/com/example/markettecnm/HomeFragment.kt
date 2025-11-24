package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
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

    private val handler = Handler(Looper.getMainLooper())
    private val delay: Long = 2000 // 2 segundos

    private val runnable = object : Runnable {
        override fun run() {
            if (bannerAdapter.itemCount > 0) {
                val current = binding.vpTendenciaCarousel.currentItem
                val next = if (current == bannerAdapter.itemCount - 1) 0 else current + 1
                binding.vpTendenciaCarousel.setCurrentItem(next, true)
            }
            handler.postDelayed(this, delay)
        }
    }

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

    override fun onResume() {
        super.onResume()
        if (::bannerAdapter.isInitialized && bannerAdapter.itemCount > 1) {
            handler.postDelayed(runnable, delay)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
    }

    private fun setupRecyclerViews() {
        // 1. TENDENCIAS (CARRUSEL)
        bannerAdapter = BannerAdapter(emptyList()) { product ->
            val intent = Intent(requireContext(), ProductDetailActivity::class.java)
            intent.putExtra("product_id", product.id)
            startActivity(intent)
        }
        binding.vpTendenciaCarousel.adapter = bannerAdapter

        binding.vpTendenciaCarousel.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    handler.removeCallbacks(runnable)
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    handler.removeCallbacks(runnable)
                    handler.postDelayed(runnable, delay)
                }
            }
        })


        // 2. CATEGORÍAS
        categoryAdapter = CategoryAdapter(emptyList()) { category ->
            openCategoryResults(category.name)
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

    private fun openCategoryResults(categoryName: String) {
        val intent = Intent(requireContext(), ResultadoCategoriaActivity::class.java).apply {
            putExtra("category_name", categoryName)
        }
        startActivity(intent)
    }


    private fun fetchData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val deferredCategories = async { RetrofitClient.instance.getCategories() }
                val deferredProducts = async { RetrofitClient.instance.getProducts() }
                val deferredReviews = async { RetrofitClient.instance.getReviews() }
                val deferredFavorites = async { RetrofitClient.instance.getFavorites() }

                val resCategories = deferredCategories.await()
                val resProducts = deferredProducts.await()
                val resReviews = deferredReviews.await()
                val resFavorites = deferredFavorites.await()

                withContext(Dispatchers.Main) {
                    val allProducts = resProducts.body() ?: emptyList()
                    val allReviews = resReviews.body() ?: emptyList()

                    // 🛑 PASO 1: Calcular el promedio de rating UNA SOLA VEZ
                    val reviewsByProduct = allReviews.groupBy { it.product }
                    val globalProductRatings = reviewsByProduct.mapValues { (_, reviewList) ->
                        reviewList.map { it.rating }.average()
                    }


                    // A. Categorías
                    if (resCategories.isSuccessful) {
                        categoryAdapter.updateCategories(resCategories.body() ?: emptyList())
                    }

                    // B. Favoritos
                    val favoriteIds = if (resFavorites.isSuccessful && resFavorites.body() != null) {
                        resFavorites.body()!!.map { it.product.id }
                    } else {
                        emptyList()
                    }
                    productAdapter.preloadFavorites(favoriteIds)

                    // C. LÓGICA DE FILTRADO Y ORDENAMIENTO
                    if (resProducts.isSuccessful && resReviews.isSuccessful) {

                        // 2. Distribuir el mapa de ratings a AMBOS adaptadores
                        productAdapter.updateRatings(globalProductRatings)
                        bannerAdapter.updateRatings(globalProductRatings) // Asignamos ratings al carrusel

                        // 3. Tendencias (Carrusel)
                        calculateTrends(allProducts, globalProductRatings)
                        if (bannerAdapter.itemCount > 1) {
                            handler.postDelayed(runnable, delay)
                        }

                        // 4. PRODUCTOS RECOMENDADOS (Lista vertical)
                        val recommendedList = filterAndSortRecommended(allProducts, globalProductRatings)
                        productAdapter.updateProducts(recommendedList)
                    } else if (resProducts.isSuccessful) {
                        // Si no hay reviews, al menos mostramos la lista completa
                        productAdapter.updateProducts(allProducts)
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error cargando datos", e)
            }
        }
    }

    // 🛑 FUNCIÓN MODIFICADA: Ahora recibe el mapa de scores ya calculado
    private fun filterAndSortRecommended(products: List<ProductModel>, productScores: Map<Int, Double>): List<ProductModel> {
        if (productScores.isEmpty()) return emptyList()

        // 1. Filtrar productos con promedio menor a 4.0
        val eligibleProductIds = productScores.filterValues { it >= 4.0 }.keys

        if (eligibleProductIds.isEmpty()) return emptyList()

        // 2. Obtener los IDs de los productos elegibles y ordenarlos por rating (de mayor a menor)
        val sortedProductIds = productScores.entries
            .filter { it.key in eligibleProductIds }
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }

        // 3. Filtrar los productos originales para obtener los objetos completos y mantener el orden
        val recommendedProducts = products
            .filter { it.id in sortedProductIds }
            .sortedBy { sortedProductIds.indexOf(it.id) }

        Log.d("RECOMENDACION", "Mostrando ${recommendedProducts.size} productos con >= 4.0 estrellas.")

        return recommendedProducts
    }


    // 🛑 FUNCIÓN MODIFICADA: Ahora recibe el mapa de scores ya calculado
    private fun calculateTrends(products: List<ProductModel>, productRatings: Map<Int, Double>) {
        if (productRatings.isEmpty()) return

        val top3Ids = productRatings.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        val topProducts = products.filter {
            it.id in top3Ids
        }

        bannerAdapter.updateBanners(topProducts)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(runnable)
        _binding = null
    }
}