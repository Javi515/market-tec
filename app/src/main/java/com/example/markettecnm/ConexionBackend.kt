package com.example.markettecnm

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.markettecnm.network.ApiClient
import com.example.markettecnm.network.ApiService
import com.example.markettecnm.network.LoginRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class ConexionBackend : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.conexion_backend)

        val btnProbar = findViewById<Button>(R.id.btnProbarConexion)
        val tvResultado = findViewById<TextView>(R.id.tvResultado)

        btnProbar.setOnClickListener {
            tvResultado.text = "Conectando al backend..."
            cargarUsuarios(tvResultado)
        }
    }

    private fun cargarUsuarios(tv: TextView) {
        lifecycleScope.launch {
            val api = ApiClient.retrofit.create(ApiService::class.java)
            try {
                // 1) Login
                val login = api.login(LoginRequest(username = "admin", password = "1234"))
                val token = login.access

                // 2) GET /api/users/ con el token
                val users = api.getUsers("Bearer $token")

                // 3) Mostrar
                val listado = if (users.isNotEmpty()) {
                    users.joinToString("\n") { u ->
                        "👤 ${u.username}  ${u.email ?: ""}".trim()
                    }
                } else {
                    "(Sin usuarios)"
                }
                tv.text = "✅ Usuarios:\n\n$listado"

            } catch (e: HttpException) {
                tv.text = "❌ Error HTTP ${e.code()}: ${e.message()}"
            } catch (e: IOException) {
                tv.text = "❌ Error de red: verifica Wi-Fi/IP del servidor"
            } catch (e: Exception) {
                tv.text = "❌ Error: ${e.message}"
            }
        }
    }
}