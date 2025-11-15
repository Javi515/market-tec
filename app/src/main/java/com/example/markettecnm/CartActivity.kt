package com.example.markettecnm

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.CartAdapter
import com.example.markettecnm.models.Product

class CartActivity : AppCompatActivity() {

    private lateinit var rvCartProducts: RecyclerView
    private lateinit var btnOrder: Button
    private lateinit var tvEmptyCart: TextView
    private lateinit var cartAdapter: CartAdapter

    private var cartProducts = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        rvCartProducts = findViewById(R.id.rvCartProducts)
        btnOrder = findViewById(R.id.btnOrder)
        tvEmptyCart = findViewById(R.id.tvEmptyCart)

        loadCartItems()
        setupListeners()
    }

    private fun loadCartItems() {
        val cartMap = loadCartMapFromPrefs(this)
        val cartIds = cartMap.keys

        // Esta lista debería venir de un Repositorio (una fuente de datos única)
        val allProducts = listOf(
            Product(1, "Papas Francesas", 59.99, 4.0f, 120, R.drawable.papas_fritas),
            Product(2, "Teclado Mecánico", 85.00, 4.5f, 550, R.drawable.teclado),
            Product(3, "Camiseta Vintage", 25.50, 4.8f, 780, R.drawable.camiseta),
            Product(4, "Mouse Inalámbrico", 19.99, 4.2f, 320, R.drawable.mouse)
            // Añade todos tus productos aquí
        )

        cartProducts.clear()

        allProducts.forEach { product ->
            val idString = product.id.toString()
            if (cartIds.contains(idString)) {
                // Obtenemos la cantidad guardada (ej. 20)
                val quantity = cartMap[idString] ?: 1
                // Creamos una copia del producto con la cantidad correcta
                cartProducts.add(product.copy(quantityInCart = quantity))
            }
        }

        if (cartProducts.isEmpty()) {
            tvEmptyCart.visibility = View.VISIBLE
            rvCartProducts.visibility = View.GONE
            btnOrder.visibility = View.GONE
        } else {
            tvEmptyCart.visibility = View.GONE
            rvCartProducts.visibility = View.VISIBLE
            btnOrder.visibility = View.VISIBLE
            setupRecyclerView()
        }
    }

    private fun setupRecyclerView() {
        // Le pasamos al adapter la lista de productos CON la cantidad correcta
        cartAdapter = CartAdapter(
            cartProducts,
            onDeleteClickListener = { productToDelete ->
                deleteIndividualItem(productToDelete)
            },
            onImageClick = { product ->
                val intent = Intent(this, ProductDetailActivity::class.java).apply {
                    putExtra("product", product)
                }
                startActivity(intent)
            }
        )
        rvCartProducts.layoutManager = LinearLayoutManager(this)
        rvCartProducts.adapter = cartAdapter
    }

    private fun setupListeners() {
        btnOrder.setOnClickListener {
            showOrderDialog()
        }
    }

    private fun deleteIndividualItem(productToDelete: Product) {
        val cartMap = loadCartMapFromPrefs(this).toMutableMap()
        val idString = productToDelete.id.toString()

        if (cartMap.remove(idString) != null) {
            saveCartMapToPrefs(this, cartMap)
            Toast.makeText(this, "✅ ${productToDelete.name} eliminado.", Toast.LENGTH_SHORT).show()
            loadCartItems()
        } else {
            Toast.makeText(this, "Error al eliminar: Producto no encontrado.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- ¡¡FUNCIÓN MODIFICADA!! ---
    private fun showOrderDialog() {
        val selectedItems = cartAdapter.getSelectedProductsForOrder()

        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un producto para ordenar.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- CÁLCULO MANUAL (para evitar errores) ---
        var totalQuantity = 0
        var totalAmount = 0.0

        for (product in selectedItems) {
            val quantity = product.quantityInCart
            val price = product.price
            totalQuantity += quantity
            totalAmount += (price * quantity)
        }
        // --- Fin del cálculo manual ---

        AlertDialog.Builder(this)
            .setTitle("Confirmar Compra")
            .setMessage("Se van a ordenar $totalQuantity producto(s) por un total de $${String.format("%.2f", totalAmount)}. ¿Confirmas la compra?")
            .setPositiveButton("Sí, ordenar") { dialog, _ ->

                // --- INICIO: LÓGICA PARA GUARDAR LA COMPRA (¡NUEVO!) ---

                // 1. Cargar las compras *existentes*
                val existingPurchases = loadPurchasesMapFromPrefs(this).toMutableMap()

                // 2. Iterar sobre los items seleccionados para esta nueva compra
                selectedItems.forEach { product ->
                    val idString = product.id.toString()
                    val quantityToBuy = product.quantityInCart // La cantidad de este producto (ej. 20)

                    // 3. Actualizar el total de compras
                    // Si ya había comprado este producto (ej. 5), se suma la nueva cantidad (ej. 20) -> (5 + 20 = 25)
                    val oldQuantity = existingPurchases.getOrDefault(idString, 0)
                    existingPurchases[idString] = oldQuantity + quantityToBuy
                }

                // 4. Guardar el mapa de compras actualizado
                savePurchasesMapToPrefs(this, existingPurchases)

                // --- FIN: LÓGICA PARA GUARDAR LA COMPRA ---


                // (Esta parte para eliminar del carrito ya estaba bien)
                val currentCartMap = loadCartMapFromPrefs(this).toMutableMap()
                selectedItems.forEach { product ->
                    currentCartMap.remove(product.id.toString())
                }
                saveCartMapToPrefs(this, currentCartMap)

                Toast.makeText(this, "¡Compra de $totalQuantity producto(s) realizada con éxito!", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                loadCartItems() // Recargamos el carrito
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    // --- FIN DE LA FUNCIÓN MODIFICADA ---

    // --- FUNCIONES DEL CARRITO (Las que ya tenías) ---

    private fun saveCartMapToPrefs(context: Context, cartMap: Map<String, Int>) {
        val prefs = context.getSharedPreferences("cart_items", Context.MODE_PRIVATE)
        val serializedMap = cartMap.entries.joinToString(";") { "${it.key}:${it.value}" }
        prefs.edit().putString("cart_map", serializedMap).apply()
    }

    private fun loadCartMapFromPrefs(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences("cart_items", Context.MODE_PRIVATE)
        val serializedMap = prefs.getString("cart_map", "") ?: ""
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

    // --- FUNCIONES DE "MIS COMPRAS" (¡NUEVAS!) ---
    // Son idénticas a las del carrito, pero usan un archivo de guardado diferente.

    private fun savePurchasesMapToPrefs(context: Context, purchasesMap: Map<String, Int>) {
        // Usamos un nombre de archivo DIFERENTE: "my_purchases"
        val prefs = context.getSharedPreferences("my_purchases", Context.MODE_PRIVATE)
        val serializedMap = purchasesMap.entries.joinToString(";") { "${it.key}:${it.value}" }
        // Usamos una clave DIFERENTE: "purchases_map"
        prefs.edit().putString("purchases_map", serializedMap).apply()
    }

    private fun loadPurchasesMapFromPrefs(context: Context): Map<String, Int> {
        // Usamos un nombre de archivo DIFERENTE: "my_purchases"
        val prefs = context.getSharedPreferences("my_purchases", Context.MODE_PRIVATE)
        // Usamos una clave DIFERENTE: "purchases_map"
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