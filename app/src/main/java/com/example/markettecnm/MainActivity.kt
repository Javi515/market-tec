package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
// import androidx.core.view.WindowCompat // <-- ELIMINADO
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var etCorreo: TextInputEditText
    private lateinit var etContrasena: TextInputEditText
    private lateinit var btnIniciarSesion: Button
    private lateinit var tvRegistro: TextView
    private lateinit var tvOlvideContrasena: TextView // <-- 1. VARIABLE AÑADIDA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- LÍNEA DE WINDOWCOMPAT ELIMINADA ---

        setContentView(R.layout.activity_main)

        // Enlazar elementos del layout
        etCorreo = findViewById(R.id.etNombre) // <-- Corregido de etNombre
        etContrasena = findViewById(R.id.etContrasena)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        tvRegistro = findViewById(R.id.tvRegistro)

        // 2. ENLACE AÑADIDO (¡Asegúrate que el ID sea correcto en tu XML!)
        tvOlvideContrasena = findViewById(R.id.tvOlvido)

        // Botón Iniciar Sesión
        btnIniciarSesion.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val password = etContrasena.text.toString().trim()

            if (correo.isEmpty()) {
                etCorreo.error = "Ingresa tu correo"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etContrasena.error = "Ingresa tu contraseña"
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("markettec_prefs", MODE_PRIVATE)
            prefs.edit().putString("access_token", "DUMMY_TOKEN").apply()

            val intent = Intent(this@MainActivity, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Texto "Regístrate"
        tvRegistro.setOnClickListener {
            // 3. CORREGIDO: Apunta a RegistroActivity, no a ConexionBackend
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }

        // --- CÓDIGO AÑADIDO ---
        // Texto "¿Te olvidaste de tu contraseña?"
        tvOlvideContrasena.setOnClickListener {
            val intent = Intent(this, OlvidasteContrasenaActivity::class.java)
            startActivity(intent)
        }
        // --- FIN DE CÓDIGO AÑADIDO ---
    }
}