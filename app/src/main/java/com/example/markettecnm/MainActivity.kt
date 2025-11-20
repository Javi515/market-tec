package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.markettecnm.network.LoginRequestBody
import com.example.markettecnm.network.TokenResponse   // ← ¡ESTE IMPORT FALTABA!
import com.example.markettecnm.network.RetrofitClient
import com.example.markettecnm.network.ErrorResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var etCorreo: TextInputEditText
    private lateinit var etContrasena: TextInputEditText
    private lateinit var btnIniciarSesion: Button
    private lateinit var tvRegistro: TextView
    private lateinit var tvOlvideContrasena: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enlazar elementos del layout
        etCorreo = findViewById(R.id.etNombre)
        etContrasena = findViewById(R.id.etContrasena)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        tvRegistro = findViewById(R.id.tvRegistro)
        tvOlvideContrasena = findViewById(R.id.tvOlvido)

        btnIniciarSesion.setOnClickListener { attemptLogin() }

        tvRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        tvOlvideContrasena.setOnClickListener {
            startActivity(Intent(this, OlvidasteContrasenaActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val username = etCorreo.text.toString().trim()
        val password = etContrasena.text.toString()

        if (username.isEmpty()) {
            etCorreo.error = "Ingresa tu correo o nombre de usuario"
            return
        }
        if (password.isEmpty()) {
            etContrasena.error = "Ingresa tu contraseña"
            return
        }

        btnIniciarSesion.isEnabled = false

        val requestBody = LoginRequestBody(username = username, password = password)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.loginUser(requestBody)

                withContext(Dispatchers.Main) {
                    btnIniciarSesion.isEnabled = true

                    if (response.isSuccessful) {
                        val tokenResponse = response.body()
                        if (tokenResponse != null) {
                            val accessToken = tokenResponse.accessToken  // ← Ahora sí existe

                            // Guardar token
                            val prefs = getSharedPreferences("markettec_prefs", MODE_PRIVATE)
                            prefs.edit().putString("access_token", accessToken).apply()

                            Toast.makeText(this@MainActivity, "¡Inicio de sesión exitoso!", Toast.LENGTH_SHORT).show()

                            // Ir al Home
                            startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                            finish()
                        }
                    } else {
                        handleLoginApiError(response.errorBody()?.string(), response.code())
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnIniciarSesion.isEnabled = true
                    Toast.makeText(this@MainActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleLoginApiError(errorBodyString: String?, code: Int) {
        try {
            val errorResponse = Gson().fromJson(errorBodyString, ErrorResponse::class.java)
            val errorMessage = when {
                errorResponse?.detail != null -> errorResponse.detail
                code == 401 || code == 400 -> "Usuario o contraseña incorrectos"
                else -> "Error del servidor: Código $code"
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error desconocido en el servidor", Toast.LENGTH_LONG).show()
        }
    }
}