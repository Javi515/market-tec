package com.example.markettecnm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.CartAdapter
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Imports de modelos
import com.example.markettecnm.models.CreateOrderRequest
import com.example.markettecnm.models.CreateOrderItem

class CartActivity : AppCompatActivity() {

    private lateinit var rvCartProducts: RecyclerView
    private lateinit var btnOrder: Button
    private lateinit var tvEmptyCart: TextView
    private lateinit var tvTotalSummary: TextView

    private lateinit var cartAdapter: CartAdapter
    private var currentProducts = listOf<ProductModel>()
    private var currentQuantities = mapOf<String, Int>()
    private var totalAmount: Double = 0.0

    // ID del usuario logueado
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        // 1. OBTENER ID DEL USUARIO AL INICIO
        val prefs = getSharedPreferences("markettec_prefs", Context.MODE_PRIVATE)
        currentUserId = prefs.getInt("current_user_id", -1)

        rvCartProducts = findViewById(R.id.rvCartProducts)
        btnOrder = findViewById(R.id.btnOrder)
        tvEmptyCart = findViewById(R.id.tvEmptyCart)
        tvTotalSummary = findViewById(R.id.tvTotalSummary)

        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // Recargar carrito (Ahora buscará el archivo específico del usuario)
        loadCartItems()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            products = emptyList(),
            quantities = emptyMap(),
            onDeleteClick = { productToDelete -> deleteItemFromPrefs(productToDelete) },
            onProductClick = { product ->
                val intent = Intent(this, ProductDetailActivity::class.java).apply {
                    putExtra("product_id", product.id)
                }
                startActivity(intent)
            },
            onSelectionChange = { _, _ -> updateSummary() }
        )
        rvCartProducts.layoutManager = LinearLayoutManager(this)
        rvCartProducts.adapter = cartAdapter
    }

    private fun loadCartItems() {
        // Cargar mapa del carrito ESPECÍFICO DEL USUARIO
        val cartMap = loadCartMapFromPrefs(this)
        currentQuantities = cartMap
        val cartIds = cartMap.keys

        if (cartIds.isEmpty()) {
            showEmptyState()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getProducts()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val allApiProducts = response.body() ?: emptyList()
                        currentProducts = allApiProducts.filter { product ->
                            cartIds.contains(product.id.toString())
                        }

                        if (currentProducts.isEmpty()) {
                            showEmptyState()
                        } else {
                            showCartState()
                            cartAdapter.updateData(currentProducts, currentQuantities)
                            updateSummary()
                        }
                    } else {
                        Toast.makeText(this@CartActivity, "Error al cargar productos", Toast.LENGTH_SHORT).show()
                        showEmptyState()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CartActivity, "Sin conexión", Toast.LENGTH_SHORT).show()
                    showEmptyState()
                }
            }
        }
    }

    private fun updateSummary() {
        val selectedItems = cartAdapter.getSelectedProductsForOrder()
        var newTotal = 0.0
        selectedItems.forEach { (product, quantity) ->
            val price = product.price.toDoubleOrNull() ?: 0.0
            newTotal += (price * quantity)
        }
        totalAmount = newTotal

        if (totalAmount > 0) {
            btnOrder.isEnabled = true
            tvTotalSummary.text = "Total: $${String.format("%.2f", totalAmount)}"
            btnOrder.text = "Ordenar ($${String.format("%.2f", totalAmount)})"
        } else {
            btnOrder.isEnabled = false
            tvTotalSummary.text = "Total: $0.00"
            btnOrder.text = "Selecciona productos"
        }
    }

    private fun setupListeners() {
        btnOrder.setOnClickListener { showOrderDialog() }
    }

    private fun showOrderDialog() {
        val selectedItems = cartAdapter.getSelectedProductsForOrder()

        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un producto.", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUserId == -1) {
            Toast.makeText(this, "Inicia sesión para comprar.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar Compra")
            .setMessage("Total: $${String.format("%.2f", totalAmount)}\n\n¿Enviar pedido al servidor?")
            .setPositiveButton("Sí, comprar") { dialog, _ ->
                dialog.dismiss()
                sendOrderToApi(selectedItems)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun sendOrderToApi(selectedItems: List<Pair<ProductModel, Int>>) {
        btnOrder.isEnabled = false
        btnOrder.text = "Procesando..."

        val apiItems = selectedItems.map { (product, quantity) ->
            CreateOrderItem(
                productId = product.id,
                quantity = quantity
            )
        }

        // 🟢 CORRECCIÓN: Usar el nuevo nombre de parámetro 'itemsToCreate'
        val request = CreateOrderRequest(itemsToCreate = apiItems)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Enviamos la orden
                val response = RetrofitClient.instance.createOrder(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@CartActivity, "¡Pedido enviado exitosamente!", Toast.LENGTH_LONG).show()

                        // Guardar en historial local (Usando el nuevo método con ID de usuario)
                        savePurchaseHistory(selectedItems)

                        // Recargar (esto limpiará el carrito de los items comprados)
                        loadCartItems()
                    } else {
                        // Diagnóstico del error 400
                        val errorBody = response.errorBody()?.string()
                        Log.e("CART_ERROR", "Fallo: ${response.code()} | $errorBody")
                        Toast.makeText(this@CartActivity, "Error al comprar (${response.code()})", Toast.LENGTH_LONG).show()
                        btnOrder.isEnabled = true
                        btnOrder.text = "Reintentar"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CART_EXCEPTION", "Error", e)
                    Toast.makeText(this@CartActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                    btnOrder.isEnabled = true
                    btnOrder.text = "Reintentar"
                }
            }
        }
    }

    private fun deleteItemFromPrefs(productToDelete: ProductModel) {
        val cartMap = loadCartMapFromPrefs(this).toMutableMap()
        if (cartMap.remove(productToDelete.id.toString()) != null) {
            saveCartMapToPrefs(this, cartMap)
            loadCartItems()
        }
    }

    private fun savePurchaseHistory(itemsBought: List<Pair<ProductModel, Int>>) {
        val existingPurchases = loadPurchasesMapFromPrefs(this).toMutableMap()
        itemsBought.forEach { (product, quantity) ->
            val idString = product.id.toString()
            val oldQuantity = existingPurchases.getOrDefault(idString, 0)
            existingPurchases[idString] = oldQuantity + quantity
        }
        savePurchasesMapToPrefs(this, existingPurchases)

        val currentCartMap = loadCartMapFromPrefs(this).toMutableMap()
        itemsBought.forEach { (product, _) -> currentCartMap.remove(product.id.toString()) }
        saveCartMapToPrefs(this, currentCartMap)
    }

    private fun showEmptyState() {
        tvEmptyCart.visibility = View.VISIBLE
        rvCartProducts.visibility = View.GONE
        btnOrder.visibility = View.GONE
    }

    private fun showCartState() {
        tvEmptyCart.visibility = View.GONE
        rvCartProducts.visibility = View.VISIBLE
        btnOrder.visibility = View.VISIBLE
    }

    // ==============================================================
    // 🟢 MÉTODOS DE PREFERENCIAS CORREGIDOS (Dinámicos por Usuario)
    // ==============================================================

    // 1. Guardar Carrito
    private fun saveCartMapToPrefs(context: Context, cartMap: Map<String, Int>) {
        val prefsName = if (currentUserId != -1) "cart_items_$currentUserId" else "cart_items_guest"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        val serializedMap = cartMap.entries.joinToString(";") { "${it.key}:${it.value}" }
        prefs.edit().putString("cart_map", serializedMap).apply()
    }

    // 2. Leer Carrito
    private fun loadCartMapFromPrefs(context: Context): Map<String, Int> {
        val prefsName = if (currentUserId != -1) "cart_items_$currentUserId" else "cart_items_guest"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        val serializedMap = prefs.getString("cart_map", "") ?: ""
        if (serializedMap.isEmpty()) return emptyMap()

        val map = mutableMapOf<String, Int>()
        serializedMap.split(";").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) try { map[parts[0]] = parts[1].toInt() } catch (_: Exception) {}
        }
        return map
    }

    // 3. Guardar Historial de Compras (Local)
    private fun savePurchasesMapToPrefs(context: Context, purchasesMap: Map<String, Int>) {
        if (currentUserId == -1) return
        val prefsName = "my_purchases_$currentUserId"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val serializedMap = purchasesMap.entries.joinToString(";") { "${it.key}:${it.value}" }
        prefs.edit().putString("purchases_map", serializedMap).apply()
    }

    // 4. Leer Historial de Compras
    private fun loadPurchasesMapFromPrefs(context: Context): Map<String, Int> {
        if (currentUserId == -1) return emptyMap()
        val prefsName = "my_purchases_$currentUserId"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val serializedMap = prefs.getString("purchases_map", "") ?: ""
        if (serializedMap.isEmpty()) return emptyMap()
        val map = mutableMapOf<String, Int>()
        serializedMap.split(";").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) try { map[parts[0]] = parts[1].toInt() } catch (_: Exception) {}
        }
        return map
    }
}