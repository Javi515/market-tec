package com.example.markettecnm

import android.content.Intent
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
import com.example.markettecnm.R
import com.example.markettecnm.adapters.CategoryAdapter
// 🛑 IMPORTANTE: Usamos 'models' porque ahí unificamos CategoryModel
import com.example.markettecnm.models.CategoryModel
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoriesFragment : Fragment() {

    private lateinit var rvCategoryList: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_categories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvCategoryList = view.findViewById(R.id.rvCategoryList)
        setupRecyclerView()
        fetchCategories()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            // 💡 CAMBIO: Quitamos <CategoryModel> como pediste, Kotlin lo infiere.
            categories = emptyList(),
            onItemClick = { category ->
                // Mantenemos la lógica de abrir por nombre
                openCategoryResults(category.name)
            }
        )

        // Usamos un GridLayoutManager con 2 columnas
        rvCategoryList.layoutManager = GridLayoutManager(context, 2)
        rvCategoryList.adapter = categoryAdapter
    }

    // Navegación a ResultadoCategoriaActivity por nombre
    private fun openCategoryResults(categoryName: String) {
        val intent = Intent(requireContext(), ResultadoCategoriaActivity::class.java).apply {
            putExtra("category_name", categoryName)
        }
        startActivity(intent)
    }

    private fun fetchCategories() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getCategories()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // 💡 CAMBIO: Simplificado a emptyList()
                        val categories = response.body() ?: emptyList()
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