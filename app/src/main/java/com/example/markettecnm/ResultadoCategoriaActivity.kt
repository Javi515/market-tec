package com.example.markettecnm

import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ProductAdapter
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async // Necessary for parallel fetching
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.emptyList

class ResultadoCategoriaActivity : AppCompatActivity() {

    private lateinit var textResultados: TextView
    private lateinit var rvResultados: RecyclerView
    private lateinit var textNoResultados: TextView
    private lateinit var categoryName: String

    // 💡 AGREGADO: Declaración del adaptador para poder precargar favoritos
    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resultados)

        title = "Productos por Categoría"

        // Vistas
        textResultados = findViewById(R.id.textResultados)
        rvResultados = findViewById(R.id.rvResultados)
        textNoResultados = findViewById(R.id.textNoResultados)

        rvResultados.layoutManager = LinearLayoutManager(this)
        rvResultados.isNestedScrollingEnabled = false

        // Inicializar el adaptador aquí (antes de loadFilteredProducts)
        setupRecyclerView()


        // Obtener la categoría exacta (usando la nueva clave 'category_name')
        categoryName = intent.getStringExtra("category_name") ?: ""
        if (categoryName.isBlank()) {
            textResultados.text = "Error: Categoría no especificada"
            showNoResults()
            return
        }

        textResultados.text = "Explorando: $categoryName"

        loadFilteredProducts(categoryName.trim())
    }

    // 💡 NUEVA FUNCIÓN: Setup del RV con el adaptador
    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(emptyList()) { product ->
            val intent = Intent(this, ProductDetailActivity::class.java).apply {
                putExtra("product_id", product.id)
            }
            startActivity(intent)
        }
        rvResultados.adapter = productAdapter
    }


    private fun loadFilteredProducts(filterName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Carga de datos en paralelo
                val deferredProducts = async { RetrofitClient.instance.getProducts() }
                val deferredFavorites = async { RetrofitClient.instance.getFavorites() } // <-- NUEVO ASYNC

                val resProducts = deferredProducts.await()
                val resFavorites = deferredFavorites.await() // <-- ESPERAR FAVORITOS

                withContext(Dispatchers.Main) {
                    if (resProducts.isSuccessful) {
                        val allApiProducts = resProducts.body() ?: emptyList()

                        // 2. Extracción de IDs de Favoritos
                        val favoriteIds = if (resFavorites.isSuccessful && resFavorites.body() != null) {
                            resFavorites.body()!!.map { it.product.id }
                        } else {
                            emptyList()
                        }

                        // 3. Aplicamos Filtro Estricto (Local)
                        val filtered = allApiProducts.filter {
                            it.categoryName.equals(filterName, ignoreCase = true)
                        }

                        if (filtered.isEmpty()) {
                            textResultados.text = "No hay productos en $filterName"
                            showNoResults()
                        } else {
                            // 4. Precargamos los favoritos y actualizamos la lista
                            productAdapter.preloadFavorites(favoriteIds) // <--- CARGA EL CACHÉ

                            textResultados.text = "$filterName (${filtered.size} productos)"
                            showResults(filtered)
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

    private fun showResults(products: List<ProductModel>) {
        textNoResultados.visibility = View.GONE
        rvResultados.visibility = View.VISIBLE

        // Solo actualizamos los datos, ya que el adaptador ya está configurado
        productAdapter.updateProducts(products)
    }

    private fun showNoResults() {
        textNoResultados.visibility = View.VISIBLE
        rvResultados.visibility = View.GONE
    }
}