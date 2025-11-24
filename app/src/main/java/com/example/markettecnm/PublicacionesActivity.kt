package com.example.markettecnm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.example.markettecnm.adapters.PublicacionAdapter
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PublicacionesActivity : AppCompatActivity() {

    private lateinit var rvPublicaciones: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var publicacionAdapter: PublicacionAdapter

    private var currentVendorFirstName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publicaciones)

        currentVendorFirstName = getLoggedInVendorName()

        setupToolbar()

        rvPublicaciones = findViewById(R.id.rvPublicaciones)
        tvEmpty = findViewById(R.id.tvEmpty)

        setupRecyclerView()
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
        publicacionAdapter = PublicacionAdapter(emptyList()) { action, product ->
            when (action) {
                "edit" -> handleEditProduct(product)
                "delete" -> handleDeleteProduct(product)
            }
        }
        rvPublicaciones.layoutManager = LinearLayoutManager(this)
        rvPublicaciones.adapter = publicacionAdapter
    }

    // --- LÓGICA DE MANEJO DE ACCIONES ---

    private fun handleEditProduct(product: ProductModel) {
        // CORRECCIÓN 1: Llamar a la Activity de MODIFICACIÓN
        Toast.makeText(this, "Abriendo edición de: ${product.name}", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, ModificarProductoActivity::class.java).apply {
            putExtra("PRODUCT_TO_EDIT_ID", product.id)
        }
        startActivity(intent)
    }

    private fun handleDeleteProduct(product: ProductModel) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Publicación")
            .setMessage("¿Estás seguro de que quieres eliminar el producto '${product.name}'?")
            .setPositiveButton("Sí, Eliminar") { _, _ ->
                deleteProductOnServer(product.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteProductOnServer(productId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // CORRECCIÓN 2: Llama a deleteProduct (Función para eliminar productos)
                val response = RetrofitClient.instance.deleteProduct(productId)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@PublicacionesActivity, "Producto eliminado con éxito.", Toast.LENGTH_SHORT).show()
                        loadMyProducts() // Recargar la lista después de eliminar
                    } else {
                        Toast.makeText(this@PublicacionesActivity, "Error ${response.code()} al eliminar.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PublicacionesActivity, "Error de red al eliminar.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- Lógica de Carga y Filtrado ---

    private fun loadMyProducts() {
        if (currentVendorFirstName.isNullOrEmpty()) {
            showEmptyState("No se pudo identificar al vendedor. Por favor, inicia sesión nuevamente.")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getProducts()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val allProducts = response.body() ?: emptyList()

                        // FILTRADO CLAVE
                        val myProducts = allProducts.filter { product ->
                            product.vendor?.firstName == currentVendorFirstName
                        }

                        if (myProducts.isEmpty()) {
                            showEmptyState("Aún no has publicado ningún producto.")
                        } else {
                            publicacionAdapter.updateProducts(myProducts)
                            showResults()
                        }
                    } else {
                        Log.e("PUBLISH", "Error: ${response.code()}")
                        showEmptyState("Error al cargar publicaciones. Código: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("PUBLISH", "Error de red", e)
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
        tvEmpty.visibility = View.VISIBLE
        tvEmpty.text = message
    }

    private fun showResults() {
        rvPublicaciones.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
    }
}