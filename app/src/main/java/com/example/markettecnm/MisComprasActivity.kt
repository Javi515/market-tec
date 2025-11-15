package com.example.markettecnm

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
// import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ComprasAdapter
import com.example.markettecnm.models.Product
import com.google.android.material.appbar.MaterialToolbar

class MisComprasActivity : AppCompatActivity() {

    private lateinit var rvMisCompras: RecyclerView
    private lateinit var tvNoCompras: TextView
    private lateinit var comprasAdapter: ComprasAdapter

    private var comprasList = mutableListOf<Product>()

    // --- CAMBIO 1 ---
    // El nombre DEBE COINCIDIR con el usado en CartActivity
    private val PURCHASE_PREFS_NAME = "my_purchases" // Antes decía "purchase_items"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge()
        setContentView(R.layout.activity_mis_compras)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.navigationIcon = AppCompatResources.getDrawable(
            this, androidx.appcompat.R.drawable.abc_ic_ab_back_material
        )
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        rvMisCompras = findViewById(R.id.rvMisCompras)
        tvNoCompras = findViewById(R.id.tvNoCompras)

        loadPurchaseItems()
    }


    private fun loadPurchaseItems() {
        val purchaseMap = loadPurchasesFromPrefs(this)
        val purchaseIds = purchaseMap.keys


        val allProducts = listOf(
            Product(1, "Papas Francesas", 59.99, 4.0f, 120, R.drawable.papas_fritas),
            Product(2, "Teclado Mecánico", 85.00, 4.5f, 550, R.drawable.teclado),
            Product(3, "Camiseta Vintage", 25.50, 4.8f, 780, R.drawable.camiseta),
            Product(4, "Mouse Inalámbrico", 19.99, 4.2f, 320, R.drawable.mouse)

        )

        comprasList.clear()

        allProducts.forEach { product ->
            val idString = product.id.toString()
            if (purchaseIds.contains(idString)) {
                val quantity = purchaseMap[idString] ?: 1
                comprasList.add(product.copy(quantityInCart = quantity))
            }
        }

        if (comprasList.isEmpty()) {
            tvNoCompras.visibility = View.VISIBLE
            rvMisCompras.visibility = View.GONE
        } else {
            tvNoCompras.visibility = View.GONE
            rvMisCompras.visibility = View.VISIBLE
            setupRecyclerView()
        }
    }

    private fun setupRecyclerView() {
        comprasAdapter = ComprasAdapter(
            comprasList,
            onContactClick = { product ->
                // Lógica para abrir chat
            }
        )
        rvMisCompras.layoutManager = LinearLayoutManager(this)
        rvMisCompras.adapter = comprasAdapter
    }

    // Esta función LEE las compras guardadas
    private fun loadPurchasesFromPrefs(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences(PURCHASE_PREFS_NAME, Context.MODE_PRIVATE)

        // --- CAMBIO 2 ---
        // La clave ("key") DEBE COINCIDIR con la usada en CartActivity
        val serializedMap = prefs.getString("purchases_map", "") ?: "" // Antes decía "purchase_map"

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