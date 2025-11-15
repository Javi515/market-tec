package com.example.markettecnm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.CategoryAdapter
import com.example.markettecnm.models.Category

class CategoriasFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_categorias, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvCategorias = view.findViewById<RecyclerView>(R.id.rvCategoriasFull)

        // Configura el GridLayoutManager con 2 columnas
        rvCategorias.layoutManager = GridLayoutManager(requireContext(), 2)

        // Agrega padding lateral para evitar que las tarjetas se peguen a los bordes
        rvCategorias.setPadding(16, 0, 16, 0)
        rvCategorias.clipToPadding = false

        val categories = listOf(
            Category("Deportes", 0),
            Category("Hogar", 0),
            Category("Libros", 0),
            Category("Moda", 0),
            Category("Electrónica", 0)
        )

        rvCategorias.adapter = CategoryAdapter(
            categories = categories,
            layoutResId = R.layout.item_category_grid
        ) { category ->
            Toast.makeText(requireContext(), "Seleccionaste: ${category.name}", Toast.LENGTH_SHORT).show()
        }
    }
}