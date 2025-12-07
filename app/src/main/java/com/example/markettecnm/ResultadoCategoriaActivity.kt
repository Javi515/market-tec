package com.example.markettecnm

import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // ✅ Usamos lifecycleScope para mayor estabilidad
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ProductAdapter
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.emptyList

class ResultadoCategoriaActivity : AppCompatActivity() {

    private lateinit var textResultados: TextView
    private lateinit var rvResultados: RecyclerView
    private lateinit var textNoResultados: TextView

    private lateinit var categoryName: String
    private var categoryId: Int = -1 // 🟢 Variable para el ID

    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resultados)

        // Vistas
        textResultados = findViewById(R.id.textResultados)
        rvResultados = findViewById(R.id.rvResultados)
        textNoResultados = findViewById(R.id.textNoResultados)

        setupRecyclerView()

        // 1. OBTENER DATOS (Nombre e ID)
        categoryName = intent.getStringExtra("category_name") ?: ""
        categoryId = intent.getIntExtra("category_id", -1)

        title = if (categoryName.isNotEmpty()) categoryName else "Categoría"

        // Validación
        if (categoryId == -1 && categoryName.isBlank()) {
            textResultados.text = "Error: Categoría no especificada"
            showNoResults()
            return
        }

        textResultados.text = "Explorando: $categoryName"

        // Cargamos los productos usando el ID prioritariamente
        loadFilteredProducts()
    }

    private fun setupRecyclerView() {
        rvResultados.layoutManager = LinearLayoutManager(this)
        rvResultados.isNestedScrollingEnabled = false

        productAdapter = ProductAdapter(emptyList()) { product ->
            val intent = Intent(this, ProductDetailActivity::class.java).apply {
                putExtra("product_id", product.id)
            }
            startActivity(intent)
        }
        rvResultados.adapter = productAdapter
    }

    private fun loadFilteredProducts() {
        // Usamos lifecycleScope para evitar crashes si el usuario sale rápido
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Carga de datos en paralelo
                val deferredProducts = async { RetrofitClient.instance.getProducts() }
                val deferredFavorites = async { RetrofitClient.instance.getFavorites() }

                val resProducts = deferredProducts.await()
                val resFavorites = deferredFavorites.await()

                withContext(Dispatchers.Main) {
                    if (resProducts.isSuccessful) {
                        val allApiProducts = resProducts.body() ?: emptyList()

                        // 🔍 LOG DIAGNÓSTICO
                        Log.d("DEBUG_CAT", "Buscando productos con Cat ID: $categoryId (Nombre: $categoryName)")

                        // 2. FILTRADO ROBUSTO (Por ID preferentemente)
                        val filtered = allApiProducts.filter { product ->
                            // A: Si tenemos ID, filtramos por ID (Exacto)
                            if (categoryId != -1) {
                                product.category == categoryId
                            }
                            // B: Fallback por nombre si el ID falla
                            else {
                                product.categoryName.equals(categoryName, ignoreCase = true)
                            }
                        }

                        // 3. Favoritos
                        val favoriteIds = if (resFavorites.isSuccessful && resFavorites.body() != null) {
                            resFavorites.body()!!.map { it.product.id }
                        } else {
                            emptyList()
                        }

                        if (filtered.isEmpty()) {
                            textResultados.text = "No hay productos en $categoryName"
                            showNoResults()
                        } else {
                            productAdapter.preloadFavorites(favoriteIds)
                            productAdapter.updateProducts(filtered)

                            textResultados.text = "$categoryName (${filtered.size} productos)"
                            showResults()
                        }
                    } else {
                        Toast.makeText(this@ResultadoCategoriaActivity, "Error al cargar catálogo", Toast.LENGTH_SHORT).show()
                        showNoResults()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CATEGORY_SEARCH", "Error de red", e)
                    Toast.makeText(this@ResultadoCategoriaActivity, "Sin conexión", Toast.LENGTH_LONG).show()
                    showNoResults()
                }
            }
        }
    }

    private fun showResults() {
        textNoResultados.visibility = View.GONE
        rvResultados.visibility = View.VISIBLE
    }

    private fun showNoResults() {
        textNoResultados.visibility = View.VISIBLE
        rvResultados.visibility = View.GONE
    }
}