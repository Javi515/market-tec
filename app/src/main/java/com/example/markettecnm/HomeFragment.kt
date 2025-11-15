package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.markettecnm.adapters.CategoryAdapter
import com.example.markettecnm.adapters.ProductAdapter
import com.example.markettecnm.adapters.TendenciaAdapter
import com.example.markettecnm.models.Category
import com.example.markettecnm.models.Product

class HomeFragment : Fragment() {

    private lateinit var vpTendenciaCarousel: ViewPager2
    private val tendenciaHandler = Handler(Looper.getMainLooper())
    private val tendenciaInterval = 3000L // 3 segundos

    private val tendenciaRunnable = object : Runnable {
        override fun run() {
            val itemCount = vpTendenciaCarousel.adapter?.itemCount ?: 0
            if (itemCount > 0) {
                val nextItem = (vpTendenciaCarousel.currentItem + 1) % itemCount
                vpTendenciaCarousel.setCurrentItem(nextItem, true)
                tendenciaHandler.postDelayed(this, tendenciaInterval)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        vpTendenciaCarousel = view.findViewById(R.id.vpTendenciaCarousel)

        val tendenciaImages = listOf(
            R.drawable.banderillas,
            R.drawable.papas_fritas,
            R.drawable.xbox
        )

        vpTendenciaCarousel.adapter = TendenciaAdapter(tendenciaImages)

        // Inicia el auto-scroll del carrusel
        tendenciaHandler.postDelayed(tendenciaRunnable, tendenciaInterval)

        val rvCategories: RecyclerView = view.findViewById(R.id.rvCategories)
        rvCategories.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val categories = listOf(
            Category("Deportes", 0),
            Category("Hogar", 0),
            Category("Libros", 0),
            Category("Moda", 0),
            Category("Electrónica", 0)
        )

        rvCategories.adapter = CategoryAdapter(
            categories = categories,
            layoutResId = R.layout.item_category_horizontal
        ) { category ->
            Toast.makeText(context, "Categoría: ${category.name}", Toast.LENGTH_SHORT).show()
        }

        val rvProducts: RecyclerView = view.findViewById(R.id.rvProducts)
        rvProducts.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        val products = listOf(
            Product(1, "Papas Francesas", 59.99, 4.0f, 24, R.drawable.papas_fritas),
            Product(2, "Teclado Mecánico", 85.00, 4.5f, 150, R.drawable.teclado),
            Product(3, "Camiseta Vintage", 25.50, 4.8f, 78, R.drawable.camiseta),
            Product(4, "Mouse Inalámbrico", 19.99, 4.2f, 95, R.drawable.mouse)
        )

        rvProducts.adapter = ProductAdapter(
            products = products,
            onClick = { product ->
                val intent = Intent(context, ProductDetailActivity::class.java).apply {
                    putExtra("product", product)
                }
                startActivity(intent)
            },
            context = context,
            onFavoriteChanged = {
                (activity as? HomeActivity)?.refreshFavorites()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tendenciaHandler.removeCallbacks(tendenciaRunnable)
    }
}