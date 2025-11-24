package com.example.markettecnm

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat // Import necesario para ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ComprasAdapter
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar // Import necesario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MisComprasActivity : AppCompatActivity() {

    private lateinit var rvMisCompras: RecyclerView
    private lateinit var tvNoCompras: TextView
    private lateinit var comprasAdapter: ComprasAdapter

    private var comprasList = mutableListOf<ProductModel>()
    private var comprasQuantities = mapOf<String, Int>()

    private val PURCHASE_PREFS_NAME = "my_purchases"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_compras)

        // CORRECCIÓN 1: Manejo seguro del findViewById y uso del apply scope.
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar?.apply {
            navigationIcon = ContextCompat.getDrawable( // Usamos ContextCompat, más seguro
                this@MisComprasActivity,
                androidx.appcompat.R.drawable.abc_ic_ab_back_material
            )
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }

        // CORRECCIÓN 2: Asegurarnos que la variable del adaptador se inicialice antes
        rvMisCompras = findViewById(R.id.rvMisCompras)
        tvNoCompras = findViewById(R.id.tvNoCompras)

        loadPurchaseItems()
    }

    // --- Funciones de Lógica Asíncrona ---

    private fun loadPurchaseItems() {
        val purchaseMap = loadPurchasesFromPrefs(this)
        comprasQuantities = purchaseMap
        val purchaseIds = purchaseMap.keys

        if (purchaseIds.isEmpty()) {
            showEmptyState()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getProducts()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val allProducts = response.body() ?: emptyList()

                        // 3. Filtrar: Obtener los detalles de los productos comprados
                        val purchasedProducts = allProducts.filter { product ->
                            purchaseIds.contains(product.id.toString())
                        }

                        comprasList.clear()
                        comprasList.addAll(purchasedProducts)

                        if (comprasList.isEmpty()) {
                            showEmptyState()
                        } else {
                            showResults() // Llama a setupRecyclerView
                        }
                    } else {
                        Toast.makeText(this@MisComprasActivity, "Error al cargar catálogo", Toast.LENGTH_SHORT).show()
                        showEmptyState()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MisComprasActivity, "Error de red", Toast.LENGTH_SHORT).show()
                    showEmptyState()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        // CORRECCIÓN 3: Pasamos la lista de productos (ProductModel) y el mapa de cantidades
        comprasAdapter = ComprasAdapter(
            comprasList,
            comprasQuantities,
            onContactClick = { product ->
                // Lógica para abrir chat
                Toast.makeText(this, "Contactar vendedor de ${product.name}", Toast.LENGTH_SHORT).show()
            }
        )
        rvMisCompras.layoutManager = LinearLayoutManager(this)
        rvMisCompras.adapter = comprasAdapter
    }

    // --- Funciones de Estado y Utilidad ---

    private fun showResults() {
        tvNoCompras.visibility = View.GONE
        rvMisCompras.visibility = View.VISIBLE
        setupRecyclerView()
    }

    private fun showEmptyState() {
        tvNoCompras.visibility = View.VISIBLE
        rvMisCompras.visibility = View.GONE
        tvNoCompras.text = "Aún no tienes compras realizadas."
    }

    // Función que lee las compras guardadas (Se deja igual)
    private fun loadPurchasesFromPrefs(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences(PURCHASE_PREFS_NAME, Context.MODE_PRIVATE)
        val serializedMap = prefs.getString("purchases_map", "") ?: ""

        if (serializedMap.isEmpty()) return emptyMap()

        return serializedMap.split(";")
            .mapNotNull { entryString ->
                val parts = entryString.split(":")
                if (parts.size == 2) {
                    try {
                        val id = parts[0]
                        val quantity = parts[1].toInt()
                        id to quantity
                    } catch (e: NumberFormatException) {
                        null
                    }
                } else {
                    null
                }
            }
            .toMap()
    }
}