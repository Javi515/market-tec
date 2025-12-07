package com.example.markettecnm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ComprasAdapter
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MisComprasActivity : AppCompatActivity() {

    private lateinit var rvMisCompras: RecyclerView
    private lateinit var tvNoCompras: TextView
    private lateinit var comprasAdapter: ComprasAdapter

    private var comprasList = mutableListOf<ProductModel>()
    private var comprasQuantities = mapOf<String, Int>()

    // Variable para saber quién está logueado
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_compras)

        // 1. OBTENER EL ID DEL USUARIO ACTUAL (Vital para cargar SU historial)
        val prefs = getSharedPreferences("markettec_prefs", Context.MODE_PRIVATE)
        currentUserId = prefs.getInt("current_user_id", -1)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar?.apply {
            navigationIcon = ContextCompat.getDrawable(
                this@MisComprasActivity,
                androidx.appcompat.R.drawable.abc_ic_ab_back_material
            )
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }

        rvMisCompras = findViewById(R.id.rvMisCompras)
        tvNoCompras = findViewById(R.id.tvNoCompras)

        if (currentUserId != -1) {
            loadPurchaseItems()
        } else {
            showEmptyState()
            // Mensaje opcional si entran sin sesión
            // Toast.makeText(this, "Inicia sesión para ver tus compras", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPurchaseItems() {
        // Leemos las compras DEL USUARIO ACTUAL
        val purchaseMap = loadPurchasesFromPrefs(this)
        comprasQuantities = purchaseMap
        val purchaseIds = purchaseMap.keys

        if (purchaseIds.isEmpty()) {
            showEmptyState()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Obtenemos todos los productos para filtrar los que compró este usuario
                val response = RetrofitClient.instance.getProducts()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val allProducts = response.body() ?: emptyList()

                        val purchasedProducts = allProducts.filter { product ->
                            purchaseIds.contains(product.id.toString())
                        }

                        comprasList.clear()
                        comprasList.addAll(purchasedProducts)

                        if (comprasList.isEmpty()) {
                            showEmptyState()
                        } else {
                            showResults()
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
        comprasAdapter = ComprasAdapter(
            comprasList,
            comprasQuantities,
            onContactClick = { product ->
                // Usamos la lógica "Detective" para encontrar el ID del vendedor correcto
                fetchRealVendorAndChat(product.id, product.name)
            }
        )
        rvMisCompras.layoutManager = LinearLayoutManager(this)
        rvMisCompras.adapter = comprasAdapter
    }

    // 🟢 Lógica "Detective" para encontrar ID del vendedor
    private fun fetchRealVendorAndChat(productId: Int, productName: String) {
        Toast.makeText(this, "Verificando vendedor...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getProductDetail(productId)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val fullProduct = response.body()!!

                        var finalSellerId = fullProduct.user
                        if (finalSellerId == 0 && fullProduct.vendor != null) {
                            finalSellerId = fullProduct.vendor.id
                        }

                        Log.d("CHAT_DEBUG", "ID Final para el Chat: $finalSellerId")

                        if (finalSellerId != 0) {
                            initiateChat(finalSellerId, productName)
                        } else {
                            Toast.makeText(this@MisComprasActivity, "Error: Vendedor no identificado", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@MisComprasActivity, "Error al cargar detalle", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CHAT_DEBUG", "Error al buscar detalle", e)
                }
            }
        }
    }

    // 🟢 Estrategia Híbrida (@Query + @Body) para el Chat
    private fun initiateChat(targetUserId: Int, productName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("CHAT_DEBUG", "Iniciando chat con User ID: $targetUserId")

                // Enviamos un cuerpo Dummy para que el servidor no reciba NULL
                val dummyBody = mapOf("action" to "init")

                val response = RetrofitClient.instance.startChat(targetUserId, dummyBody)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val chatData = response.body()!!
                        val intent = Intent(this@MisComprasActivity, ChatActivity::class.java).apply {
                            putExtra("conversation_id", chatData.id)
                            putExtra("chat_title", chatData.other_user ?: "Vendedor")
                            putExtra("product_name", productName)
                        }
                        startActivity(intent)
                    } else {
                        val errorBody = response.errorBody()?.string() ?: ""
                        val errorCode = response.code()

                        Log.e("CHAT_ERROR", "Código: $errorCode - Body: $errorBody")

                        if (errorCode == 404) {
                            Toast.makeText(this@MisComprasActivity, "El vendedor no tiene perfil configurado.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@MisComprasActivity, "No se pudo conectar ($errorCode)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CHAT_EXCEPTION", "Fallo total:", e)
                    Toast.makeText(this@MisComprasActivity, "Error de conexión", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

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

    // 🟢 Carga dinámica basada en el ID del usuario
    private fun loadPurchasesFromPrefs(context: Context): Map<String, Int> {
        // Usamos el ID del usuario para crear un nombre de archivo único
        val prefsName = "my_purchases_$currentUserId"

        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
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