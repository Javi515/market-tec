package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ProductAdapter
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.markettecnm.models.ProductModel
import kotlin.collections.emptyList

class ResultadosActivity : AppCompatActivity() {

    private lateinit var textResultados: TextView
    private lateinit var rvResultados: RecyclerView
    private lateinit var textNoResultados: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resultados)

        title = "Resultados de búsqueda"

        // Vistas
        textResultados = findViewById(R.id.textResultados)
        rvResultados = findViewById(R.id.rvResultados)
        textNoResultados = findViewById(R.id.textNoResultados)

        rvResultados.layoutManager = LinearLayoutManager(this)
        rvResultados.isNestedScrollingEnabled = false

        // Obtener el texto buscado
        val query = intent.getStringExtra("query") ?: ""

        // --- CAMBIO 1: Validación inicial ---
        if (query.isBlank()) {
            textResultados.text = "Búsqueda vacía"
            showNoResults()
            return
        }

        // Si la búsqueda es muy corta (1 letra), avisamos y no buscamos
        if (query.trim().length < 2) {
            textResultados.text = "Escribe al menos 2 letras para buscar"
            showNoResults()
            return
        }

        textResultados.text = "Buscando: \"$query\""

        // Cargar y filtrar productos
        searchProductsAndFilterLocally(query.trim())
    }

    // 🛑 FUNCIÓN ÚNICA: Filtro por INICIO DE PALABRA (Word Boundary)
    private fun searchProductsAndFilterLocally(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. OBTENER TODO el catálogo
                val response = RetrofitClient.instance.getProducts()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val allProducts = response.body() ?: emptyList()

                        // --- CAMBIO 2: FILTRADO ESTRICTO ---
                        val filtered = allProducts.filter { product ->
                            // Paso A: Dividir el nombre en palabras
                            // Ej: "Lavadora Samsung 20kg" -> ["Lavadora", "Samsung", "20kg"]
                            val words = product.name.split(" ")

                            // Paso B: Verificar si ALGUNA palabra empieza con la query
                            words.any { word ->
                                word.startsWith(query, ignoreCase = true)
                            }
                        }

                        if (filtered.isEmpty()) {
                            textResultados.text = "No se encontraron resultados para \"$query\""
                            showNoResults()
                        } else {
                            textResultados.text = "Resultados para \"$query\" (${filtered.size})"
                            showResults(filtered)
                        }
                    } else {
                        Toast.makeText(this@ResultadosActivity, "Error al cargar productos", Toast.LENGTH_SHORT).show()
                        showNoResults()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("SEARCH", "Error de red", e)
                    Toast.makeText(this@ResultadosActivity, "Sin conexión", Toast.LENGTH_LONG).show()
                    showNoResults()
                }
            }
        }
    }

    private fun showResults(products: List<ProductModel>) {
        textNoResultados.visibility = View.GONE
        rvResultados.visibility = View.VISIBLE

        // Pasamos la lista directamente al adaptador
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