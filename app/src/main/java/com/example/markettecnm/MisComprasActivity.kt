package com.example.markettecnm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout // Importar LinearLayout (Para el contenedor de estado vacío)
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ComprasAdapter
import com.example.markettecnm.models.OrderResponse
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MisComprasActivity : AppCompatActivity() {

    private lateinit var rvMisCompras: RecyclerView

    // DECLARACIONES CORREGIDAS: Contenedor y TextView interno
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvEmptyMessage: TextView

    private lateinit var comprasAdapter: ComprasAdapter
    private var ordersList = mutableListOf<OrderResponse>()

    // Variable para saber quién está logueado
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_compras)

        // 1. OBTENER EL ID DEL USUARIO ACTUAL
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

        // 2. INICIALIZACIÓN DE VISTAS CORREGIDA
        rvMisCompras = findViewById(R.id.rvMisCompras)
        llEmptyState = findViewById(R.id.llEmptyState)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage) // TextView dentro del contenedor

        setupRecyclerView()

        if (currentUserId != -1) {
            loadOrdersFromApi()
        } else {
            showEmptyState()
        }
    }


    private fun setupRecyclerView() {
        comprasAdapter = ComprasAdapter(
            ordersList = ordersList,
            onContactClick = { productId, productName ->
                fetchRealVendorAndChat(productId, productName)
            },
            onCancelClick = { orderId ->
                showCancelConfirmationDialog(orderId)
            }
        )
        rvMisCompras.layoutManager = LinearLayoutManager(this)
        rvMisCompras.adapter = comprasAdapter
    }


    // FUNCIÓN CORREGIDA: Muestra estado de carga
    private fun showLoadingState() {
        rvMisCompras.visibility = View.GONE
        llEmptyState.visibility = View.VISIBLE
        tvEmptyMessage.text = "Cargando órdenes..."
    }


    // FUNCIÓN DE CARGA: Obtiene las órdenes del cliente desde la API
    private fun loadOrdersFromApi() {
        showLoadingState()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getOrders()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        ordersList.clear()
                        ordersList.addAll(response.body()!!.reversed())

                        if (ordersList.isEmpty()) {
                            showEmptyState()
                        } else {
                            showResults()
                            comprasAdapter.updateData(ordersList)
                        }
                    } else {
                        Toast.makeText(this@MisComprasActivity, "Error al cargar órdenes (${response.code()})", Toast.LENGTH_SHORT).show()
                        showEmptyState()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("COMPRAS_API_ERROR", "Error de red: ${e.message}")
                    Toast.makeText(this@MisComprasActivity, "Error de red al cargar órdenes", Toast.LENGTH_SHORT).show()
                    showEmptyState()
                }
            }
        }
    }


    // Diálogo de confirmación antes de llamar a la API
    private fun showCancelConfirmationDialog(orderId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Cancelar Pedido")
            .setMessage("¿Estás seguro de que deseas cancelar la orden #$orderId? Esta acción no se puede deshacer.")
            .setPositiveButton("Sí, Cancelar") { dialog, _ ->
                cancelOrderOnApi(orderId)
                dialog.dismiss()
            }
            .setNegativeButton("No, Mantener", null)
            .show()
    }


    // FUNCIÓN CLAVE: Llama al endpoint de cancelación
    private fun cancelOrderOnApi(orderId: Int) {
        Toast.makeText(this, "Cancelando orden #$orderId...", Toast.LENGTH_LONG).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dummyBody = mapOf("action" to "cancel")

                val response = RetrofitClient.instance.cancelOrder(orderId, dummyBody)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MisComprasActivity, "✅ Orden #$orderId cancelada.", Toast.LENGTH_LONG).show()

                        loadOrdersFromApi()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("CANCEL_ERROR", "Fallo ${response.code()}: $errorBody")
                        Toast.makeText(this@MisComprasActivity, "Error: No se pudo cancelar el pedido. (${response.code()})", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CANCEL_EXCEPTION", "Error de red al cancelar", e)
                    Toast.makeText(this@MisComprasActivity, "Error de conexión.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    // Lógica "Detective" para encontrar ID del vendedor (Mantener)
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

    // Estrategia Híbrida (@Query + @Body) para el Chat (Mantener)
    private fun initiateChat(targetUserId: Int, productName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
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
                        val errorCode = response.code()
                        val msg = if (errorCode == 404) "El vendedor no tiene perfil." else "No se pudo conectar ($errorCode)"
                        Toast.makeText(this@MisComprasActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MisComprasActivity, "Error de conexión", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 🟢 Funciones de estado corregidas para usar llEmptyState y tvEmptyMessage
    private fun showResults() {
        llEmptyState.visibility = View.GONE
        rvMisCompras.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        llEmptyState.visibility = View.VISIBLE
        rvMisCompras.visibility = View.GONE
        tvEmptyMessage.text = "Aún no tienes compras realizadas."
    }
}