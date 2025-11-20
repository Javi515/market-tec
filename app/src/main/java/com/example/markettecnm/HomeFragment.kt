package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.markettecnm.adapters.CategoryAdapter
import com.example.markettecnm.adapters.ProductAdapter
import com.example.markettecnm.databinding.FragmentHomeBinding
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        fetchData()
    }

    private fun setupRecyclerViews() {
        // Categorías
        categoryAdapter = CategoryAdapter(emptyList()) { category ->
            Log.d("HomeFragment", "Categoría clickeada: ${category.name}")
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        // Productos
        productAdapter = ProductAdapter(emptyList()) { product ->
            Log.d("HomeFragment", "Producto clickeado: ${product.name}")
            val intent = Intent(requireContext(), ProductDetailActivity::class.java)
            intent.putExtra("product_id", product.id)
            startActivity(intent)
        }
        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = productAdapter
        }
    }

    private fun fetchData() {
        lifecycleScope.launch {
            try {
                // CATEGORÍAS
                val categoryResponse = RetrofitClient.instance.getCategories()
                if (categoryResponse.isSuccessful) {
                    val categories = categoryResponse.body() ?: emptyList()
                    categoryAdapter.updateCategories(categories)
                }

                // PRODUCTOS
                val productResponse = RetrofitClient.instance.getProducts()
                if (productResponse.isSuccessful) {
                    val products = productResponse.body() ?: emptyList()
                    productAdapter = ProductAdapter(products) { product ->
                        val intent = Intent(requireContext(), ProductDetailActivity::class.java)
                        intent.putExtra("product_id", product.id)
                        startActivity(intent)
                    }
                    binding.rvProducts.adapter = productAdapter
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error al cargar datos", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}