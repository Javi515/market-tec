package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BannedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_banned)

        val reason = intent.getStringExtra("ban_reason") ?: "Incumplimiento de los términos y condiciones."

        val tvReason = findViewById<TextView>(R.id.tvBanReason)
        tvReason.text = "Razón: $reason"

        findViewById<Button>(R.id.btnBackToLogin).setOnClickListener {
            // Regresar al Login y limpiar pila de actividades
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // Bloquear botón "Atrás" para que no puedan saltarse la pantalla
    override fun onBackPressed() {
        // No hacer nada (super.onBackPressed() eliminado)
    }
}