package com.example.markettecnm

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ResultadosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resultados)
        title = "Resultados"

        val query = intent.getStringExtra("query") ?: ""
        findViewById<TextView>(R.id.textResultados).text =
            "Mostrando resultados para: $query"
    }
}
