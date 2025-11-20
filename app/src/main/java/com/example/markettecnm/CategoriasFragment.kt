package com.example.markettecnm

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
// Importación de R corregida para acceder a los recursos (ej. R.layout.fragment_categories)
import com.example.markettecnm.R
import com.example.markettecnm.adapters.CategoryAdapter
import com.example.markettecnm.network.CategoryModel
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoriesFragment : Fragment() {

    // Se recomienda usar el binding para acceder a las vistas sin findViewById,
    // pero mantendremos RecyclerView para compatibilidad con tu código.
    private lateinit var rvCategoryList: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // CORRECCIÓN 1: R.layout.fragment_categories ya es accesible gracias a la importación.
        return inflater.inflate(R.layout.fragment_categories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvCategoryList = view.findViewById(R.id.rvCategoryList)
        setupRecyclerView()
        fetchCategories()
    }

    private fun setupRecyclerView() {
        // Inicialización explícita del adaptador con la lista vacía tipada
        categoryAdapter = CategoryAdapter(
            categories = emptyList<CategoryModel>(),
            onItemClick = { category ->
                // Acción al hacer clic en una categoría
                Toast.makeText(context, "Navegando a productos de: ${category.name}", Toast.LENGTH_SHORT).show()
            }
        )

        // Usamos un GridLayoutManager con 2 columnas
        rvCategoryList.layoutManager = GridLayoutManager(context, 2)
        rvCategoryList.adapter = categoryAdapter
    }

    private fun fetchCategories() {
        // Usamos viewLifecycleOwner.lifecycleScope para un manejo seguro de coroutines
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Llamada a la API
                val response = RetrofitClient.instance.getCategories()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val categories = response.body() ?: emptyList<CategoryModel>()

                        // CORRECCIÓN 2: Llama a la función 'updateCategories' del adaptador
                        categoryAdapter.updateCategories(categories)
                    } else {
                        Log.e("API_CALL", "Error Cat: ${response.code()}")
                        Toast.makeText(context, "Error al cargar categorías.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("NETWORK", "Excepción al cargar categorías: ${e.message}")
                }
            }
        }
    }
}