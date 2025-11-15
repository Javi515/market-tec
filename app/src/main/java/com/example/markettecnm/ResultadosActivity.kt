package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ProductAdapter // <-- Asegúrate de importar tu adapter
import com.example.markettecnm.models.Product // <-- Asegúrate de importar tu modelo

class ResultadosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resultados)
        title = "Resultados"

        // --- 1. Obtener Vistas ---
        val textResultados: TextView = findViewById(R.id.textResultados)
        val rvResultados: RecyclerView = findViewById(R.id.rvResultados)
        val textNoResultados: TextView = findViewById(R.id.textNoResultados)

        // --- 2. Obtener el query de búsqueda ---
        val query = intent.getStringExtra("query") ?: ""
        textResultados.text = "Mostrando resultados para: \"$query\""

        // --- 3. Obtener la lista COMPLETA de productos ---
        // (Por ahora, la definimos aquí. Idealmente vendría de una base de datos o un 'Repositorio')
        val allProducts = listOf(
            Product(1, "Papas Francesas", 59.99, 4.0f, 24, R.drawable.papas_fritas),
            Product(2, "Teclado Mecánico", 85.00, 4.5f, 150, R.drawable.teclado),
            Product(3, "Camiseta Vintage", 25.50, 4.8f, 78, R.drawable.camiseta),
            Product(4, "Mouse Inalámbrico", 19.99, 4.2f, 95, R.drawable.mouse)
            // Agrega aquí TODOS los productos que tengas
        )

        // --- 4. Filtrar la lista ---
        val filteredProducts = allProducts.filter { product ->
            // Busca si el nombre del producto contiene el texto buscado (ignorando mayúsculas/minúsculas)
            product.name.contains(query, ignoreCase = true)
        }

        // --- 5. Configurar el RecyclerView o mostrar mensaje de "No resultados" ---
        if (filteredProducts.isEmpty()) {
            // No se encontró nada
            rvResultados.visibility = View.GONE
            textNoResultados.visibility = View.VISIBLE
        } else {
            // Se encontraron productos, mostramos la lista
            rvResultados.visibility = View.VISIBLE
            textNoResultados.visibility = View.GONE

            rvResultados.layoutManager = LinearLayoutManager(this)

            // Usamos el MISMO ProductAdapter que usas en HomeFragment
            rvResultados.adapter = ProductAdapter(
                products = filteredProducts,
                onClick = { product ->
                    // Abrir detalles del producto
                    val intent = Intent(this, ProductDetailActivity::class.java).apply {
                        putExtra("product", product)
                    }
                    startActivity(intent)
                },
                context = this,
                onFavoriteChanged = {
                    // Esta activity no es HomeActivity, así que no podemos
                    // llamar a 'refreshFavorites' directamente.
                    // Por ahora lo dejamos vacío o podrías usar un Toast.
                }
            )
        }
    }
}