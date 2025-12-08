package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // ✅ Importante para evitar crashes
import com.example.markettecnm.models.LoginRequestBody
import com.example.markettecnm.network.RetrofitClient
import com.example.markettecnm.network.ErrorResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
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

        // ✅ USAMOS lifecycleScope: Es más seguro que crear un CoroutineScope manual
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. LOGIN: Obtener Token
                val response = RetrofitClient.instance.loginUser(requestBody)

                if (response.isSuccessful && response.body() != null) {
                    val accessToken = response.body()!!.access

                    // Guardar Token TEMPORALMENTE
                    val prefs = getSharedPreferences("markettec_prefs", MODE_PRIVATE)
                    prefs.edit().putString("access_token", accessToken).apply()

                    // 2. OBTENER PERFIL: Verificar Baneo y Datos
                    try {
                        val profileResponse = RetrofitClient.instance.getMyProfile()

                        withContext(Dispatchers.Main) {
                            if (profileResponse.isSuccessful && profileResponse.body() != null) {
                                val userProfile = profileResponse.body()!!

                                // 🛑 VERIFICACIÓN DE BANEO
                                // Ahora sí detecta 'isBanned' porque ya actualizamos Models.kt
                                val isBanned = userProfile.profile?.isBanned == true

                                if (isBanned) {
                                    // 🚨 USUARIO BANEADO
                                    // 1. Borrar token para cerrar la sesión a medias
                                    prefs.edit().clear().apply()

                                    // 2. Obtener razón del baneo
                                    val banReason = userProfile.profile?.banReason ?: "Cuenta suspendida."

                                    // 3. Ir a Pantalla de Baneo
                                    val intent = Intent(this@MainActivity, BannedActivity::class.java)
                                    intent.putExtra("ban_reason", banReason)
                                    startActivity(intent)

                                    // Reactivamos el botón por si regresan
                                    btnIniciarSesion.isEnabled = true
                                } else {
                                    // ✅ USUARIO ACTIVO (Flujo Normal)
                                    prefs.edit().apply {
                                        putInt("current_user_id", userProfile.id)
                                        putString("current_user_first_name", userProfile.firstName)
                                        apply()
                                    }

                                    Toast.makeText(this@MainActivity, "¡Bienvenido ${userProfile.firstName}!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                                    finish() // Cerramos el login
                                }
                            } else {
                                // Error al obtener perfil
                                prefs.edit().clear().apply()
                                btnIniciarSesion.isEnabled = true
                                Toast.makeText(this@MainActivity, "No se pudo cargar el perfil.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            prefs.edit().clear().apply()
                            btnIniciarSesion.isEnabled = true
                            Log.e("LOGIN_PROFILE", "Error perfil", e)
                            Toast.makeText(this@MainActivity, "Error al verificar cuenta.", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    // Error en el Login (Usuario/Contra incorrectos)
                    withContext(Dispatchers.Main) {
                        btnIniciarSesion.isEnabled = true
                        handleLoginApiError(response.errorBody()?.string(), response.code())
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnIniciarSesion.isEnabled = true
                    Log.e("LOGIN_ERROR", "Error conexión", e)
                    Toast.makeText(this@MainActivity, "Error de conexión con el servidor", Toast.LENGTH_LONG).show()
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