package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.markettecnm.network.LoginRequestBody
import com.example.markettecnm.network.TokenResponse
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
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        coroutineScope.launch {
            try {
                val response = RetrofitClient.instance.loginUser(requestBody)

                withContext(Dispatchers.Main) {
                    btnIniciarSesion.isEnabled = true

                    if (response.isSuccessful) {
                        val tokenResponse = response.body()
                        if (tokenResponse != null) {
                            val accessToken = tokenResponse.accessToken

                            // 1. Guardar Token inmediatamente
                            val prefs = getSharedPreferences("markettec_prefs", MODE_PRIVATE)
                            prefs.edit().putString("access_token", accessToken).apply()

                            // 2. HACEMOS LLAMADA ADICIONAL PARA OBTENER EL NOMBRE DE PILA (FirstName)
                            // Ejecutamos el perfil fetch en una nueva corrutina o la misma
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val profileResponse = RetrofitClient.instance.getMyProfile()

                                    withContext(Dispatchers.Main) {
                                        if (profileResponse.isSuccessful) {
                                            val profile = profileResponse.body()
                                            profile?.let { userProfile ->
                                                // 3. GUARDAMOS EL NOMBRE Y ID ÚNICO DEL VENDEDOR
                                                prefs.edit().apply {
                                                    // Guardamos el ID único (seguro para el futuro)
                                                    putInt("current_user_id", userProfile.id)
                                                    // Guardamos el nombre de pila para el filtro visual en Publicaciones
                                                    putString("current_user_first_name", userProfile.firstName)
                                                    apply()
                                                }

                                                Toast.makeText(this@MainActivity, "Inicio de sesión exitoso!", Toast.LENGTH_SHORT).show()
                                                startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                                                finish()

                                            } ?: throw Exception("Datos de perfil vacíos.")
                                        } else {
                                            // Si falla el perfil (ej. 404), limpiamos el token y avisamos
                                            prefs.edit().clear().apply()
                                            Toast.makeText(this@MainActivity, "Fallo al obtener datos de perfil. Reintenta.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Log.e("LOGIN_PROFILE", "Error de red al obtener perfil.", e)
                                        Toast.makeText(this@MainActivity, "Error de red al obtener perfil.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }

                        }
                    } else {
                        handleLoginApiError(response.errorBody()?.string(), response.code())
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnIniciarSesion.isEnabled = true
                    Log.e("LOGIN_ERROR", "Error de conexión: ${e.message}")
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