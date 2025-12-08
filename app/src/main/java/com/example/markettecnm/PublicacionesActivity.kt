package com.example.markettecnm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout // Importar si usas llEmptyState
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.PublicacionAdapter
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PublicacionesActivity : AppCompatActivity() {

    private lateinit var rvPublicaciones: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvEmpty: TextView

    private lateinit var publicacionAdapter: PublicacionAdapter
    private var publicacionesList = mutableListOf<ProductModel>() // Lista local

    private var currentVendorFirstName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publicaciones)

        // 1. Obtener Nombre del Vendedor (Lógica que te funcionaba)
        currentVendorFirstName = getLoggedInVendorName()

        setupToolbar()

        rvPublicaciones = findViewById(R.id.rvPublicaciones)
        llEmptyState = findViewById(R.id.llEmptyState)
        tvEmpty = findViewById(R.id.tvEmpty)

        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadMyProducts()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar?.apply {
            navigationIcon = ContextCompat.getDrawable(
                this@PublicacionesActivity,
                androidx.appcompat.R.drawable.abc_ic_ab_back_material
            )
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
    }

    private fun setupRecyclerView() {
        // CALLBACK ACTUALIZADO: Maneja IDs de menú (Int)
        publicacionAdapter = PublicacionAdapter(publicacionesList) { actionId, product ->
            when (actionId) {
                R.id.action_view_product_detail -> {
                    val intent = Intent(this, ProductDetailActivity::class.java).apply {
                        putExtra("product_id", product.id)
                    }
                    startActivity(intent)
                }
                R.id.action_edit_product -> handleEditProduct(product)
                R.id.action_delete_product -> handleDeleteProduct(product)
                // NUEVA ACCIÓN
                R.id.action_mark_sold_out -> showSoldOutConfirmation(product)
            }
        }
        rvPublicaciones.layoutManager = LinearLayoutManager(this)
        rvPublicaciones.adapter = publicacionAdapter
    }

    // --- ACCIONES ---

    private fun handleEditProduct(product: ProductModel) {
        val intent = Intent(this, ModificarProductoActivity::class.java).apply {
            putExtra("PRODUCT_TO_EDIT_ID", product.id)
        }
        startActivity(intent)
    }

    private fun handleDeleteProduct(product: ProductModel) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Publicación")
            .setMessage("¿Estás seguro de eliminar '${product.name}'?")
            .setPositiveButton("Sí, Eliminar") { _, _ ->
                deleteProductOnServer(product.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Diálogo para Agotar
    private fun showSoldOutConfirmation(product: ProductModel) {
        AlertDialog.Builder(this)
            .setTitle("Marcar como Agotado")
            .setMessage("¿Marcar '${product.name}' como agotado?")
            .setPositiveButton("Sí, Agotar") { _, _ ->
                markProductAsSoldOut(product)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- LLAMADAS A LA API ---

    private fun deleteProductOnServer(productId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.deleteProduct(productId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@PublicacionesActivity, "Producto eliminado.", Toast.LENGTH_SHORT).show()
                        loadMyProducts()
                    } else {
                        Toast.makeText(this@PublicacionesActivity, "Error al eliminar.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@PublicacionesActivity, "Error de red.", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // 🟢 FUNCIÓN CORREGIDA: Ahora pasa product.name como segundo parámetro
    private fun markProductAsSoldOut(product: ProductModel) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Aquí agregamos product.name para satisfacer el parámetro @Query("q")
                val response = RetrofitClient.instance.markProductAsSoldOut(product.id, product.name, product)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@PublicacionesActivity, "Marcado como Agotado.", Toast.LENGTH_SHORT).show()
                        loadMyProducts() // Recargar para ver el cambio de estatus
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("API_ERROR", "Error al agotar: ${response.code()} | $errorBody")
                        Toast.makeText(this@PublicacionesActivity, "Error al actualizar.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("NETWORK_ERROR", "Excepción", e)
                    Toast.makeText(this@PublicacionesActivity, "Error de red.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- CARGA DE PRODUCTOS (Lógica Restaurada) ---

    private fun loadMyProducts() {
        if (currentVendorFirstName.isNullOrEmpty()) {
            showEmptyState("No se pudo identificar al vendedor. Inicia sesión.")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getProducts()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val allProducts = response.body() ?: emptyList()

                        // FILTRADO ORIGINAL POR NOMBRE (El que funciona)
                        val myProducts = allProducts.filter { product ->
                            product.vendor?.firstName == currentVendorFirstName
                        }

                        publicacionesList.clear()
                        publicacionesList.addAll(myProducts)

                        if (publicacionesList.isEmpty()) {
                            showEmptyState("Aún no has publicado ningún producto.")
                        } else {
                            showResults()
                            publicacionAdapter.updateProducts(publicacionesList)
                        }
                    } else {
                        showEmptyState("Error al cargar. Código: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showEmptyState("Error de conexión.")
                }
            }
        }
    }

    private fun getLoggedInVendorName(): String? {
        val sessionPrefs = getSharedPreferences("markettec_prefs", Context.MODE_PRIVATE)
        return sessionPrefs.getString("current_user_first_name", null)
    }

    private fun showEmptyState(message: String) {
        rvPublicaciones.visibility = View.GONE
        llEmptyState.visibility = View.VISIBLE
        tvEmpty.text = message
    }

    private fun showResults() {
        llEmptyState.visibility = View.GONE
        rvPublicaciones.visibility = View.VISIBLE
    }
}