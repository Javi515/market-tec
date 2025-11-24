package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.os.Handler // <-- IMPORTANTE
import android.os.Looper // <-- IMPORTANTE
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2 // Necesario para el carrusel
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

    // Variables para la rotación automática
    private val handler = Handler(Looper.getMainLooper())
    private val delay: Long = 2000 // 2 segundos

    // Runnable que se ejecutará cada 2 segundos
    private val runnable = object : Runnable {
        override fun run() {
            // Lógica para avanzar la página
            if (bannerAdapter.itemCount > 0) {
                val current = binding.vpTendenciaCarousel.currentItem
                val next = if (current == bannerAdapter.itemCount - 1) 0 else current + 1
                binding.vpTendenciaCarousel.setCurrentItem(next, true)
            }
            // Programar la próxima ejecución
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

    // 🛑 CICLO DE VIDA: Iniciar la rotación al reanudar el fragmento
    override fun onResume() {
        super.onResume()
        if (bannerAdapter.itemCount > 1) {
            handler.postDelayed(runnable, delay)
        }
    }

    // 🛑 CICLO DE VIDA: Detener la rotación al pausar el fragmento
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

        // Listener para reiniciar la rotación si el usuario desliza
        binding.vpTendenciaCarousel.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    // Si el usuario toca, detenemos el auto-scroll
                    handler.removeCallbacks(runnable)
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    // Si termina de deslizar, reiniciamos el auto-scroll
                    handler.removeCallbacks(runnable)
                    handler.postDelayed(runnable, delay)
                }
            }
        })


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
                // ... (Carga de datos) ...
                // Nota: Tu código de carga de productos y reviews está aquí.
                // Lo mantengo sin modificar para no romper la lógica de API.

                // ... (El bloque con deferredCategories, deferredProducts, deferredReviews) ...

                val deferredCategories = async { RetrofitClient.instance.getCategories() }
                val deferredProducts = async { RetrofitClient.instance.getProducts() }
                val deferredReviews = async { RetrofitClient.instance.getReviews() }
                val deferredFavorites = async { RetrofitClient.instance.getFavorites() }

                val resCategories = deferredCategories.await()
                val resProducts = deferredProducts.await()
                val resReviews = deferredReviews.await()
                val resFavorites = deferredFavorites.await()

                withContext(Dispatchers.Main) {
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

                    // C. Productos
                    val allProducts = resProducts.body() ?: emptyList()
                    if (resProducts.isSuccessful) {
                        productAdapter.updateProducts(allProducts)
                    }

                    // D. Tendencias
                    if (resReviews.isSuccessful && resProducts.isSuccessful) {
                        val allReviews = resReviews.body() ?: emptyList()
                        calculateTrends(allProducts, allReviews)

                        // 🛑 INICIAR ROTACIÓN SOLO DESPUÉS DE CARGAR DATOS
                        if (bannerAdapter.itemCount > 1) {
                            handler.postDelayed(runnable, delay)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error cargando datos", e)
            }
        }
    }

    private fun calculateTrends(products: List<ProductModel>, reviews: List<ReviewModel>) {
        if (reviews.isEmpty()) return

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

        val topProducts = products.filter {
            it.id in top3Ids
        }

        bannerAdapter.updateBanners(topProducts)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Detener el handler para evitar crashes al salir del fragmento
        handler.removeCallbacks(runnable)
        _binding = null
    }
}