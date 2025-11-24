package com.example.markettecnm

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar

class MisVentasActivity : AppCompatActivity() {

    private lateinit var tvVentasPlaceholder: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_ventas)

        setupToolbar()

        // Inicializamos la vista placeholder
        tvVentasPlaceholder = findViewById(R.id.tvVentasPlaceholder)

        // Mostrar mensaje de placeholder ya que la API de ventas no existe
        tvVentasPlaceholder.text = "Aún no tienes órdenes de venta.\n¡Publica tu primer producto!"

        // Dado que no tenemos RecyclerView ni API para leer, el código se detiene aquí,
        // mostrando el placeholder correctamente.
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        // Hacemos el manejo seguro del Toolbar
        toolbar?.apply {
            title = "Mis Ventas"
            // Utilizamos ContextCompat para acceder al ícono de forma segura
            navigationIcon = ContextCompat.getDrawable(
                this@MisVentasActivity,
                androidx.appcompat.R.drawable.abc_ic_ab_back_material
            )
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
    }
}