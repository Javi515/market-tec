package com.example.markettecnm

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.markettecnm.models.Product
import com.google.android.material.textfield.TextInputEditText

class ProductDetailActivity : AppCompatActivity() {

    private var quantity = 1
    private lateinit var etQuantity: TextInputEditText
    private lateinit var tvPrice: TextView
    private lateinit var product: Product

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        product = intent.getSerializableExtra("product") as? Product ?: run {
            finish()
            return
        }

        val ivProductImage = findViewById<ImageView>(R.id.ivProductImage)
        val tvProductName = findViewById<TextView>(R.id.tvProductName)
        tvPrice = findViewById(R.id.tvPrice)
        etQuantity = findViewById(R.id.etQuantity)
        val btnAddToCart = findViewById<Button>(R.id.btnAddToCart)
        val btnIncrease = findViewById<Button>(R.id.btnIncrease)
        val btnDecrease = findViewById<Button>(R.id.btnDecrease)

        ivProductImage.setImageResource(product.imageRes)
        tvProductName.text = product.name
        tvPrice.text = "$${String.format("%.2f", product.price)}"

        updateQuantityDisplay()

        btnIncrease.setOnClickListener {
            quantity++
            updateQuantityDisplay()
        }

        btnDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateQuantityDisplay()
            }
        }

        btnAddToCart.setOnClickListener {
            addToCart(product.id, quantity)
            Toast.makeText(this, "✅ ${product.name} (x$quantity) agregado al carrito.", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateQuantityDisplay() {
        etQuantity.setText(quantity.toString())
    }

    private fun addToCart(productId: Int, count: Int) {
        val sharedPrefs = getSharedPreferences("cart_items", Context.MODE_PRIVATE)
        val serializedMap = sharedPrefs.getString("cart_map", "") ?: ""

        val cartMap = serializedMap.split(";")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val id = parts[0]
                    val quantity = parts[1].toIntOrNull() ?: return@mapNotNull null
                    id to quantity
                } else null
            }
            .toMap()
            .toMutableMap()

        val currentQty = cartMap[productId.toString()] ?: 0
        cartMap[productId.toString()] = currentQty + count

        val newSerialized = cartMap.entries.joinToString(";") { "${it.key}:${it.value}" }
        sharedPrefs.edit().putString("cart_map", newSerialized).apply()
    }
}