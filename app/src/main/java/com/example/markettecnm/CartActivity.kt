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

class CartActivity : AppCompatActivity() {

    private lateinit var rvCartProducts: RecyclerView
    private lateinit var btnOrder: Button
    private lateinit var tvEmptyCart: TextView
    private lateinit var tvTotalSummary: TextView // Asumo que tienes un TextView para el total

    private lateinit var cartAdapter: CartAdapter
    private var currentProducts = listOf<ProductModel>()
    private var currentQuantities = mapOf<String, Int>()
    private var totalAmount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        rvCartProducts = findViewById(R.id.rvCartProducts)
        btnOrder = findViewById(R.id.btnOrder)
        tvEmptyCart = findViewById(R.id.tvEmptyCart)
        // Si no tienes este ID en el XML, se lo puedes asignar a otro TextView o al botón mismo.
        tvTotalSummary = findViewById(R.id.tvTotalSummary)

        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadCartItems() // Recargar cada vez que la Activity vuelve al frente
    }

    private fun setupRecyclerView() {
        // Inicialización del adapter con todos los handlers
        cartAdapter = CartAdapter(
            products = emptyList(),
            quantities = emptyMap(),
            onDeleteClick = { productToDelete ->
                deleteItemFromPrefs(productToDelete)
            },
            onProductClick = { product ->
                val intent = Intent(this, ProductDetailActivity::class.java).apply {
                    putExtra("product_id", product.id)
                }
                startActivity(intent)
            },
            // Listener crucial para actualizar el total cuando se selecciona/deselecciona un item
            onSelectionChange = { _, _ -> updateSummary() }
        )
        rvCartProducts.layoutManager = LinearLayoutManager(this)
        rvCartProducts.adapter = cartAdapter
    }

    private fun loadCartItems() {
        // 1. Leer el mapa de IDs y cantidades del teléfono
        val cartMap = loadCartMapFromPrefs(this)
        currentQuantities = cartMap
        val cartIds = cartMap.keys

        if (cartIds.isEmpty()) {
            showEmptyState()
            return
        }

        // 2. Descargar TODOS los productos de la API para filtrar
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getProducts()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val allApiProducts = response.body() ?: emptyList()

                        // 3. Filtrar: Solo los productos cuyos IDs están en nuestro carrito
                        currentProducts = allApiProducts.filter { product ->
                            cartIds.contains(product.id.toString())
                        }

                        if (currentProducts.isEmpty()) {
                            showEmptyState()
                        } else {
                            showCartState()
                            cartAdapter.updateData(currentProducts, currentQuantities)
                            updateSummary() // Actualizar el total por primera vez
                        }
                    } else {
                        Toast.makeText(this@CartActivity, "Error al cargar productos del catálogo", Toast.LENGTH_SHORT).show()
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
        // Obtenemos la lista de productos seleccionados y sus cantidades desde el adaptador
        val selectedItems = cartAdapter.getSelectedProductsForOrder()

        var newTotal = 0.0
        var totalQuantity = 0

        selectedItems.forEach { (product, quantity) ->
            val price = product.price.toDoubleOrNull() ?: 0.0
            newTotal += (price * quantity)
            totalQuantity += quantity
        }

        totalAmount = newTotal

        if (totalAmount > 0) {
            btnOrder.isEnabled = true
            tvTotalSummary.text = "Total: $${String.format("%.2f", totalAmount)}"
            btnOrder.text = "Ordenar Compra ($${String.format("%.2f", totalAmount)})"
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
            Toast.makeText(this, "Selecciona al menos un producto para ordenar.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar Compra")
            .setMessage("Se van a ordenar producto(s) por un total de $${String.format("%.2f", totalAmount)}. ¿Confirmas la compra?")
            .setPositiveButton("Sí, ordenar") { dialog, _ ->

                // LÓGICA DE COMPRA (Mandar a API o guardar historial local)
                // Por ahora, solo guardamos el historial local y limpiamos el carrito

                savePurchaseHistory(selectedItems)

                Toast.makeText(this, "¡Compra exitosa! 🎉", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                loadCartItems() // Recargamos para mostrar el carrito vacío/actualizado
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteItemFromPrefs(productToDelete: ProductModel) {
        val cartMap = loadCartMapFromPrefs(this).toMutableMap()
        val idString = productToDelete.id.toString()

        if (cartMap.remove(idString) != null) {
            saveCartMapToPrefs(this, cartMap)
            Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
            loadCartItems() // Recargar la lista
        }
    }

    // Lógica para guardar la compra en Historial (Similar a la que tenías)
    private fun savePurchaseHistory(itemsBought: List<Pair<ProductModel, Int>>) {
        val existingPurchases = loadPurchasesMapFromPrefs(this).toMutableMap()

        itemsBought.forEach { (product, quantity) ->
            val idString = product.id.toString()
            val oldQuantity = existingPurchases.getOrDefault(idString, 0)
            existingPurchases[idString] = oldQuantity + quantity
        }

        savePurchasesMapToPrefs(this, existingPurchases)

        // Limpiamos los ítems comprados del carrito
        val currentCartMap = loadCartMapFromPrefs(this).toMutableMap()
        itemsBought.forEach { (product, _) ->
            currentCartMap.remove(product.id.toString())
        }
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
        btnOrder.isEnabled = totalAmount > 0
    }

    // --- UTILS DE PREFERENCIAS (Necesarias para la lógica) ---

    private fun saveCartMapToPrefs(context: Context, cartMap: Map<String, Int>) {
        val prefs = context.getSharedPreferences("cart_items", Context.MODE_PRIVATE)
        val serializedMap = cartMap.entries.joinToString(";") { "${it.key}:${it.value}" }
        prefs.edit().putString("cart_map", serializedMap).apply()
    }

    private fun loadCartMapFromPrefs(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences("cart_items", Context.MODE_PRIVATE)
        val serializedMap = prefs.getString("cart_map", "") ?: ""
        if (serializedMap.isEmpty()) return emptyMap()

        val map = mutableMapOf<String, Int>()
        serializedMap.split(";").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                try {
                    map[parts[0]] = parts[1].toInt()
                } catch (e: Exception) { }
            }
        }
        return map
    }

    private fun savePurchasesMapToPrefs(context: Context, purchasesMap: Map<String, Int>) {
        val prefs = context.getSharedPreferences("my_purchases", Context.MODE_PRIVATE)
        val serializedMap = purchasesMap.entries.joinToString(";") { "${it.key}:${it.value}" }
        prefs.edit().putString("purchases_map", serializedMap).apply()
    }

    private fun loadPurchasesMapFromPrefs(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences("my_purchases", Context.MODE_PRIVATE)
        val serializedMap = prefs.getString("purchases_map", "") ?: ""
        if (serializedMap.isEmpty()) return emptyMap()

        val map = mutableMapOf<String, Int>()
        serializedMap.split(";").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                try {
                    map[parts[0]] = parts[1].toInt()
                } catch (e: Exception) { }
            }
        }
        return map
    }
}