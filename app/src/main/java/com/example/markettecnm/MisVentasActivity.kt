package com.example.markettecnm

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
import com.example.markettecnm.adapters.VentasAdapter
import com.example.markettecnm.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MisVentasActivity : AppCompatActivity() {

    private lateinit var rvMisVentas: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var ventasAdapter: VentasAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_ventas)

        setupToolbar()

        // Enlazamos las vistas
        rvMisVentas = findViewById(R.id.rvMisVentas)
        tvEmpty = findViewById(R.id.tvEmpty)

        setupRecyclerView()
        loadMySales()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar?.apply {
            title = "Mis Ventas"
            navigationIcon = ContextCompat.getDrawable(
                this@MisVentasActivity,
                androidx.appcompat.R.drawable.abc_ic_ab_back_material
            )
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
    }

    private fun setupRecyclerView() {
        rvMisVentas.layoutManager = LinearLayoutManager(this)

        // Inicializamos con lista vacía y el listener del clic
        ventasAdapter = VentasAdapter(emptyList()) { clientId, productName ->
            if (clientId != 0) {
                initiateChat(clientId, productName)
            } else {
                Toast.makeText(this, "Error: ID de cliente inválido", Toast.LENGTH_SHORT).show()
            }
        }
        rvMisVentas.adapter = ventasAdapter
    }

    private fun loadMySales() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getMySales()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val sales = response.body() ?: emptyList()

                        if (sales.isNotEmpty()) {
                            tvEmpty.visibility = View.GONE
                            rvMisVentas.visibility = View.VISIBLE

                            // Actualizamos el adaptador con los datos nuevos
                            // NOTA: Es mejor actualizar la lista interna si el adapter lo soporta,
                            // pero recrearlo funciona para salir del paso.
                            ventasAdapter = VentasAdapter(sales) { clientId, productName ->
                                initiateChat(clientId, productName)
                            }
                            rvMisVentas.adapter = ventasAdapter
                        } else {
                            rvMisVentas.visibility = View.GONE
                            tvEmpty.visibility = View.VISIBLE
                            tvEmpty.text = "Aún no tienes órdenes de venta.\n¡Publica tu primer producto!"
                        }
                    } else {
                        Toast.makeText(this@MisVentasActivity, "Error al cargar ventas: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("VENTAS", "Error de conexión", e)
                    Toast.makeText(this@MisVentasActivity, "Sin conexión al servidor", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 🟢 LÓGICA DE CHAT CORREGIDA
    private fun initiateChat(targetUserId: Int, productName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("CHAT_DEBUG", "Iniciando chat con Cliente ID: $targetUserId")

                val dummyBody = mapOf("action" to "init")
                val response = RetrofitClient.instance.startChat(targetUserId, dummyBody)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val chatData = response.body()!!

                        // 🟢 CORRECCIÓN AQUÍ:
                        // 1. Usamos 'otherUser' (camelCase)
                        // 2. Obtenemos el nombre o el username
                        val chatTitle = chatData.otherUser.firstName ?: chatData.otherUser.username ?: "Cliente"

                        val intent = Intent(this@MisVentasActivity, ChatActivity::class.java).apply {
                            putExtra("conversation_id", chatData.id)
                            putExtra("chat_title", chatTitle) // Título corregido
                            putExtra("product_name", productName)
                        }
                        startActivity(intent)
                    } else {
                        val errorCode = response.code()
                        Log.e("CHAT_ERROR", "Error: $errorCode")

                        if (errorCode == 404) {
                            Toast.makeText(this@MisVentasActivity, "El cliente no tiene perfil configurado.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@MisVentasActivity, "No se pudo conectar ($errorCode)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CHAT_EXCEPTION", "Error", e)
                    Toast.makeText(this@MisVentasActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}