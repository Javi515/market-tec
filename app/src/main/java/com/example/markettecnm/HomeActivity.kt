package com.example.markettecnm

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import android.content.Intent

class HomeActivity : AppCompatActivity() {

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.contentContainer, fragment)
            .commit()
    }

    private fun setActiveTab(tab: String) {
        // Barra de búsqueda
        val searchBar = findViewById<LinearLayout>(R.id.searchBarContainer)
        val searchEditText = findViewById<EditText>(R.id.searchEditText)

        // Iconos y textos
        val iconHome = findViewById<ImageView>(R.id.iconHome)
        val labelHome = findViewById<TextView>(R.id.labelHome)
        val iconCategorias = findViewById<ImageView>(R.id.iconCategorias)
        val labelCategorias = findViewById<TextView>(R.id.labelCategorias)
        val iconCuenta = findViewById<ImageView>(R.id.iconCuenta)
        val labelCuenta = findViewById<TextView>(R.id.labelCuenta)

        // Reset de selección
        iconHome.isSelected = false; labelHome.isSelected = false
        iconCategorias.isSelected = false; labelCategorias.isSelected = false
        iconCuenta.isSelected = false; labelCuenta.isSelected = false

        when (tab) {
            "home" -> {
                iconHome.isSelected = true; labelHome.isSelected = true
                searchBar.visibility = View.VISIBLE
                searchEditText.hint = "Buscar productos..."
            }
            "categorias" -> {
                iconCategorias.isSelected = true; labelCategorias.isSelected = true
                searchBar.visibility = View.VISIBLE
                searchEditText.hint = "Buscar categorías..."
            }
            "cuenta" -> {
                iconCuenta.isSelected = true; labelCuenta.isSelected = true
                searchBar.visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        val favButton = findViewById<ImageButton>(R.id.favButton)
        val cartButton = findViewById<ImageButton>(R.id.cartButton)
        val navInicio = findViewById<LinearLayout>(R.id.navInicio)
        val navCategorias = findViewById<LinearLayout>(R.id.navCategorias)
        val navCuenta = findViewById<LinearLayout>(R.id.navCuenta)

        // Home por defecto
        if (savedInstanceState == null) {
            showFragment(HomeFragment())
            setActiveTab("home")
        }

        // Buscar -> Resultados
        searchEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString().trim()
                if (query.isNotEmpty()) {
                    val i = Intent(this@HomeActivity, ResultadosActivity::class.java)
                    i.putExtra("query", query)
                    startActivity(i)
                } else {
                    Toast.makeText(this, "Escribe algo para buscar", Toast.LENGTH_SHORT).show()
                }
                true
            } else false
        }

        // Botones superiores (placeholder)
        favButton.setOnClickListener {
            Toast.makeText(this, "Favoritos (próximamente)", Toast.LENGTH_SHORT).show()
        }
        cartButton.setOnClickListener {
            Toast.makeText(this, "Carrito (próximamente)", Toast.LENGTH_SHORT).show()
        }

        // Bottom nav
        navInicio.setOnClickListener {
            showFragment(HomeFragment())
            setActiveTab("home")
        }
        navCategorias.setOnClickListener {
            showFragment(CategoriasFragment())
            setActiveTab("categorias")
        }
        navCuenta.setOnClickListener {
            showFragment(CuentaFragment())
            setActiveTab("cuenta")
        }
    }
}
