package com.example.markettecnm

import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ProductAdapter
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.emptyList

class ResultadoCategoriaActivity : AppCompatActivity() {

    private lateinit var textResultados: TextView
    private lateinit var rvResultados: RecyclerView
    private lateinit var textNoResultados: TextView
    private lateinit var categoryName: String // Nombre de la categoría a filtrar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resultados) // Reutilizamos el layout de resultados

        title = "Productos por Categoría"

        // Vistas
        textResultados = findViewById(R.id.textResultados)
        rvResultados = findViewById(R.id.rvResultados)
        textNoResultados = findViewById(R.id.textNoResultados)

        rvResultados.layoutManager = LinearLayoutManager(this)
        rvResultados.isNestedScrollingEnabled = false

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

    private fun loadFilteredProducts(filterName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Obtener TODO el catálogo (temporalmente, por el bug del API)
                val response = RetrofitClient.instance.getProducts()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val allProducts = response.body() ?: emptyList()

                        // 🛑 FILTRO CLAVE: Solo productos cuya categoría coincida EXACTAMENTE
                        val filtered = allProducts.filter {
                            it.categoryName.equals(filterName, ignoreCase = true)
                        }

                        if (filtered.isEmpty()) {
                            textResultados.text = "No hay productos en $filterName"
                            showNoResults()
                        } else {
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

        rvResultados.adapter = ProductAdapter(products) { product ->
            val intent = Intent(this, ProductDetailActivity::class.java).apply {
                putExtra("product_id", product.id)
            }
            startActivity(intent)
        }
    }

    private fun showNoResults() {
        textNoResultados.visibility = View.VISIBLE
        rvResultados.visibility = View.GONE
    }
}