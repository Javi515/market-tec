package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var etCorreo: TextInputEditText
    private lateinit var etContrasena: TextInputEditText
    private lateinit var btnIniciarSesion: Button
    private lateinit var tvRegistro: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enlazar elementos del layout
        etCorreo = findViewById(R.id.etNombre) // mismo id que en el XML
        etContrasena = findViewById(R.id.etContrasena)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        tvRegistro = findViewById(R.id.tvRegistro)

        // Botón Iniciar Sesión (ahora sin conexión al backend)
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

            // Guardar un token falso opcional para mantener compatibilidad
            val prefs = getSharedPreferences("markettec_prefs", MODE_PRIVATE)
            prefs.edit().putString("access_token", "DUMMY_TOKEN").apply()

            // Ir directamente al HomeActivity
            val intent = Intent(this@MainActivity, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Texto "Regístrate"
        tvRegistro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }
}
