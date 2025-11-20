package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment

// --- IMPORTACIONES AÑADIDAS ---
import android.app.AlertDialog
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import androidx.cardview.widget.CardView
// -----------------------------

class HomeActivity : AppCompatActivity() {

    // Fragments
    private lateinit var homeFragment: HomeFragment
    private lateinit var favoritesFragment: FavoritesFragment
    // NOTA: Asegúrate de que CategoriasFragment y CuentaFragment existan

    // Views
    private lateinit var searchBar: LinearLayout
    private lateinit var searchEditText: EditText
    private lateinit var favButton: ImageButton
    private lateinit var cartButton: ImageButton // Botón del carrito
    // ... (El resto de las Views del Bottom Nav)
    private lateinit var navInicio: LinearLayout
    private lateinit var navCategorias: LinearLayout
    private lateinit var navCuenta: LinearLayout
    private lateinit var iconHome: ImageView
    private lateinit var labelHome: TextView
    private lateinit var iconCategorias: ImageView
    private lateinit var labelCategorias: TextView
    private lateinit var iconCuenta: ImageView
    private lateinit var labelCuenta: TextView

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun setActiveTab(tab: String) {
        // Reset
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
            // NOTA: Considera qué hacer si el usuario regresa de CartActivity
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- MODIFICACIÓN 1: HABILITAR EDGE-TO-EDGE (PANTALLA COMPLETA) ---
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // ------------------------------------------------------------------

        setContentView(R.layout.activity_home)

        // Inicializar views
        searchBar = findViewById(R.id.searchBarContainer)
        searchEditText = findViewById(R.id.searchEditText)
        favButton = findViewById(R.id.favButton)
        cartButton = findViewById(R.id.cartButton) // Referencia al botón del carrito
        // ... (El resto de las inicializaciones)
        navInicio = findViewById(R.id.navInicio)
        navCategorias = findViewById(R.id.navCategorias)
        navCuenta = findViewById(R.id.navCuenta)
        iconHome = findViewById(R.id.iconHome)
        labelHome = findViewById(R.id.labelHome)
        iconCategorias = findViewById(R.id.iconCategorias)
        labelCategorias = findViewById(R.id.labelCategorias)
        iconCuenta = findViewById(R.id.iconCuenta)
        labelCuenta = findViewById(R.id.labelCuenta)


        // --- CÓDIGO AÑADIDO PARA CÍRCULOS ---
        // (Asegúrate de que tu activity_home.xml tenga IDs para los CardView)
        val card1: CardView = findViewById(R.id.armandoCard)
        val card2: CardView = findViewById(R.id.javiCard)
        val card3: CardView = findViewById(R.id.fedeCard)
        val card4: CardView = findViewById(R.id.alex_marinCard)

        val image1: ImageView = findViewById(R.id.circleImage1)
        val image2: ImageView = findViewById(R.id.circleImage2)
        val image3: ImageView = findViewById(R.id.circleImage3)
        val image4: ImageView = findViewById(R.id.circleImage4)

        // Asignar los clics
        card1.setOnClickListener { mostrarDialogoImagen(image1.drawable) }
        card2.setOnClickListener { mostrarDialogoImagen(image2.drawable) }
        card3.setOnClickListener { mostrarDialogoImagen(image3.drawable) }
        card4.setOnClickListener { mostrarDialogoImagen(image4.drawable) }
        // --- FIN DE CÓDIGO AÑADIDO ---


        // Instanciar fragments
        homeFragment = HomeFragment()
        favoritesFragment = FavoritesFragment()

        // Mostrar Home por defecto
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.contentContainer, homeFragment)
                .commit()
            setActiveTab("home")
        }

        // Búsqueda
        searchEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString().trim()
                if (query.isNotEmpty()) {
                    val intent = Intent(this, ResultadosActivity::class.java).apply {
                        putExtra("query", query)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Escribe algo para buscar", Toast.LENGTH_SHORT).show()
                }
                true
            } else false
        }

        // Botón de favoritos → abre pantalla de favoritos
        favButton.setOnClickListener {
            showFragment(favoritesFragment)
            setActiveTab("favorites") // Asumi...
        }

        // --- MODIFICACIÓN 2: FUNCIONALIDAD DEL BOTÓN DEL CARRITO ---
        cartButton.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }
        // -----------------------------------------------------------

        // Bottom Navigation
        navInicio.setOnClickListener {
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            supportFragmentManager.beginTransaction()
                .replace(R.id.contentContainer, homeFragment)
                .commit()
            setActiveTab("home")
        }

        navCategorias.setOnClickListener {
            // NOTA: Asegúrate de que CategoriasFragment exista
            showFragment(CategoriesFragment())
            setActiveTab("categorias")
        }

        navCuenta.setOnClickListener {
            // NOTA: Asegúrate de que CuentaFragment exista
            showFragment(CuentaFragment())
            setActiveTab("cuenta")
        }
    }



    // --- FUNCIÓN AÑADIDA PARA MOSTRAR DIÁLOGO ---
    // (Asegúrate de que R.layout.dialogo_vista_previa exista)
    private fun mostrarDialogoImagen(drawable: Drawable?) {
        if (drawable == null) return

        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)

        // Inflar el layout del diálogo
        val dialogView = inflater.inflate(R.layout.dialogo_vista_previa, null)
        val ivVistaPrevia: ImageView = dialogView.findViewById(R.id.ivVistaPrevia)

        // Poner la imagen del círculo en el diálogo
        ivVistaPrevia.setImageDrawable(drawable)

        builder.setView(dialogView)
        val dialog = builder.create()

        // Hacer que el fondo del diálogo sea transparente
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Hacer que se cierre al tocar la vista (el fondo o la imagen)
        dialogView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    // --- FIN DE LA FUNCIÓN AÑADIDA ---
}