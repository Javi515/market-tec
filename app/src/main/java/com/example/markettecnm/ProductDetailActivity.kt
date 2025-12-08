package com.example.markettecnm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.markettecnm.adapters.ReviewAdapter
import com.example.markettecnm.network.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 👇 IMPORTS CORREGIDOS (Todos vienen de 'models')
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.models.ReviewModel
import com.example.markettecnm.network.CreateReviewRequest // ✅ CORRECCIÓN
import com.example.markettecnm.network.UpdateReviewRequest // ✅ CORRECCIÓN

class ProductDetailActivity : AppCompatActivity() {

    private var quantity = 1
    private lateinit var product: ProductModel

    private lateinit var ivProductImage: ImageView
    private lateinit var tvProductName: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvVendorName: TextView
    private lateinit var tvVendorCareer: TextView
    private lateinit var ivVendorImage: ImageView
    private lateinit var etQuantity: TextInputEditText
    private lateinit var btnIncrease: Button
    private lateinit var btnDecrease: Button
    private lateinit var btnAddToCart: Button
    private lateinit var rvReviews: RecyclerView
    private lateinit var etComment: TextInputEditText
    private lateinit var ratingBarInput: RatingBar
    private lateinit var btnSubmitReview: Button

    // Obtener el nombre del usuario logueado para validaciones
    private val currentUserName: String by lazy {
        getSharedPreferences("markettec_prefs", MODE_PRIVATE).getString("current_user_first_name", "Usuario") ?: "Usuario"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        val productId = intent.getIntExtra("product_id", -1)
        if (productId == -1) {
            Toast.makeText(this, "Error: ID no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findAllViews()
        setupClickListeners()
        loadProduct(productId)
    }

    private fun findAllViews() {
        ivProductImage = findViewById(R.id.ivProductImage)
        tvProductName = findViewById(R.id.tvProductName)
        tvPrice = findViewById(R.id.tvPrice)
        tvDescription = findViewById(R.id.tvDescription)
        tvVendorName = findViewById(R.id.tvVendorName)
        tvVendorCareer = findViewById(R.id.tvVendorCareer)
        ivVendorImage = findViewById(R.id.ivVendorImage)
        etQuantity = findViewById(R.id.etQuantity)
        btnIncrease = findViewById(R.id.btnIncrease)
        btnDecrease = findViewById(R.id.btnDecrease)
        btnAddToCart = findViewById(R.id.btnAddToCart)
        rvReviews = findViewById(R.id.rvReviews)
        etComment = findViewById(R.id.etComment)
        ratingBarInput = findViewById(R.id.ratingBarInput)
        btnSubmitReview = findViewById(R.id.btnSubmitReview)

        rvReviews.layoutManager = LinearLayoutManager(this)
    }

    private fun setupClickListeners() {
        btnIncrease.setOnClickListener { quantity++; updateQuantityDisplay() }
        btnDecrease.setOnClickListener { if (quantity > 1) quantity--; updateQuantityDisplay() }

        btnAddToCart.setOnClickListener {
            if (::product.isInitialized) {
                // Llama a la función que guarda en el carrito del usuario específico
                addToCart(product.id, quantity)
                Toast.makeText(this, "✅ ${product.name} (x$quantity) agregado al carrito", Toast.LENGTH_LONG).show()
            }
        }

        btnSubmitReview.setOnClickListener { submitReview() }

        ratingBarInput.setOnRatingBarChangeListener { ratingBar, rating, _ ->
            if (rating < 1.0f) {
                ratingBar.rating = 1.0f
            }
        }
    }

    private fun loadProduct(productId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getProductDetail(productId)

                if (response.isSuccessful && response.body() != null) {
                    product = response.body()!!
                    bindProductData()
                    loadReviews(product.id)
                } else {
                    Toast.makeText(this@ProductDetailActivity, "No se pudo cargar el producto", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProductDetailActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun bindProductData() {
        tvProductName.text = product.name
        tvPrice.text = "$${product.price}"
        tvDescription.text = product.description.ifEmpty { "Sin descripción" }

        val imgUrl = product.image

        if (!imgUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imgUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(ivProductImage)

            ivProductImage.setOnClickListener {
                try {
                    val intent = Intent(this, FullScreenImageActivity::class.java)
                    intent.putExtra("image_url", imgUrl)
                    startActivity(intent)
                } catch (e: Exception) {
                    // Ignorar
                }
            }
        } else {
            ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        val vendor = product.vendor
        if (vendor != null) {
            tvVendorName.text = vendor.firstName ?: "Vendedor"
            tvVendorCareer.text = vendor.career ?: "Estudiante"

            if (!vendor.profileImage.isNullOrEmpty()) {
                Glide.with(this)
                    .load(vendor.profileImage)
                    .circleCrop()
                    .into(ivVendorImage)
            }
        } else {
            tvVendorName.text = "Market Tec"
            tvVendorCareer.text = "Administrador"
        }
        updateQuantityDisplay()
    }

    private fun updateQuantityDisplay() {
        etQuantity.setText(quantity.toString())
    }

    // 🟢 CARRITO CORREGIDO: Guarda por ID de usuario
    private fun addToCart(productId: Int, count: Int) {
        val userPrefs = getSharedPreferences("markettec_prefs", Context.MODE_PRIVATE)
        val userId = userPrefs.getInt("current_user_id", -1)

        val prefsName = if (userId != -1) "cart_items_$userId" else "cart_items_guest"

        val sp = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val current = sp.getString("cart_map", "") ?: ""
        val map = mutableMapOf<String, Int>()

        if (current.isNotEmpty()) {
            current.split(";").forEach {
                val parts = it.split(":")
                if (parts.size == 2) {
                    try {
                        map[parts[0]] = parts[1].toInt()
                    } catch (e: Exception) {}
                }
            }
        }

        map[productId.toString()] = (map[productId.toString()] ?: 0) + count
        sp.edit().putString("cart_map", map.entries.joinToString(";") { "${it.key}:${it.value}" }).apply()
    }

    private fun loadReviews(productId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getReviews()
                if (response.isSuccessful) {
                    val reviews = response.body()?.filter { it.product == productId } ?: emptyList()
                    rvReviews.adapter = ReviewAdapter(reviews, currentUserName) { action, review ->
                        when (action) {
                            "edit" -> showEditDialog(review)
                            "delete" -> deleteReview(review.id)
                        }
                    }
                }
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    private fun submitReview() {
        if (!::product.isInitialized) return

        val comment = etComment.text.toString().trim()
        val rating = ratingBarInput.rating.toInt()

        if (comment.isEmpty()) {
            Toast.makeText(this, "Escribe un comentario", Toast.LENGTH_SHORT).show()
            return
        }
        if (rating == 0) {
            Toast.makeText(this, "Selecciona las estrellas", Toast.LENGTH_SHORT).show()
            return
        }

        // Validación visual simple (Idealmente comparar IDs)
        val isMyProduct = product.vendor.firstName == currentUserName
        if (isMyProduct) {
            Toast.makeText(this, "No puedes reseñar tus propios productos.", Toast.LENGTH_LONG).show()
            return
        }

        val request = CreateReviewRequest(
            product = product.id,
            rating = rating,
            comment = comment
        )

        btnSubmitReview.isEnabled = false
        btnSubmitReview.text = "Enviando..."

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.postReview(request)
                }

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ProductDetailActivity, "¡Reseña enviada!", Toast.LENGTH_LONG).show()
                        etComment.text?.clear()
                        ratingBarInput.rating = 1f
                        loadReviews(product.id)
                    } else {
                        Toast.makeText(this@ProductDetailActivity, "Error al enviar [API].", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProductDetailActivity, "Sin conexión", Toast.LENGTH_SHORT).show()
            } finally {
                withContext(Dispatchers.Main) {
                    btnSubmitReview.isEnabled = true
                    btnSubmitReview.text = "Enviar reseña"
                }
            }
        }
    }

    private fun showEditDialog(review: ReviewModel) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_review, null)
        val etEditComment = dialogView.findViewById<EditText>(R.id.etEditComment)
        val ratingBarEdit = dialogView.findViewById<RatingBar>(R.id.ratingBarEdit)

        etEditComment.setText(review.comment)
        ratingBarEdit.post { ratingBarEdit.rating = review.rating.toFloat() }

        AlertDialog.Builder(this)
            .setTitle("Editar reseña")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val newComment = etEditComment.text.toString().trim()
                val newRating = ratingBarEdit.rating.toInt()
                if (newComment.isNotEmpty() && newRating > 0) {
                    updateReview(review.id, newRating, newComment)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateReview(reviewId: Int, rating: Int, comment: String) {
        lifecycleScope.launch {
            try {
                // 🟢 AQUÍ ESTABA EL ERROR: Ahora usa el objeto correcto importado de 'models'
                val request = UpdateReviewRequest(
                    id = reviewId,
                    product = product.id,
                    rating = rating,
                    comment = comment
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.updateReview(reviewId, request)
                }

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ProductDetailActivity, "Reseña actualizada ⭐", Toast.LENGTH_SHORT).show()
                        loadReviews(product.id)
                    } else {
                        Log.e("REVIEW_UPDATE_API", "FALLO: Código ${response.code()}")
                        Toast.makeText(this@ProductDetailActivity, "Error al actualizar.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("REVIEW_UPDATE_NET", "Error de red: ${e.message}")
                Toast.makeText(this@ProductDetailActivity, "Sin conexión.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteReview(reviewId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar")
            .setMessage("¿Eliminar reseña?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            RetrofitClient.instance.deleteReview(reviewId)
                        }
                        if (response.isSuccessful) {
                            loadReviews(product.id)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@ProductDetailActivity, "Error de red", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}