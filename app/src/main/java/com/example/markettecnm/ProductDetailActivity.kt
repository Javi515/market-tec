package com.example.markettecnm

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.markettecnm.adapters.ReviewAdapter
// IMPORTANTE: Importamos el modelo correcto de la carpeta models
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.models.ReviewModel
import com.example.markettecnm.network.* // Para los Requests (CreateReviewRequest, etc)
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ProductDetailActivity : AppCompatActivity() {

    private var quantity = 1

    // CORRECCIÓN 1: Usamos ProductModel (el que definimos en models.kt)
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

    // 👇 Usuario actual (cámbialo por el de tu sesión real si lo tienes)
    private val currentUserName: String = "Usuario"

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
            // Verificamos que product esté inicializado antes de usarlo
            if (::product.isInitialized) {
                addToCart(product.id, quantity)
                Toast.makeText(this, "✅ ${product.name} (x$quantity) agregado al carrito", Toast.LENGTH_LONG).show()
            }
        }
        btnSubmitReview.setOnClickListener { submitReview() }
    }

    private fun loadProduct(productId: Int) {
        lifecycleScope.launch {
            try {
                // Ahora getProductDetail devuelve Response<ProductModel>
                val response = RetrofitClient.instance.getProductDetail(productId)

                if (response.isSuccessful && response.body() != null) {
                    // CORRECCIÓN: La asignación ahora funciona porque los tipos coinciden
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
        tvPrice.text = "S/ ${product.price}"
        tvDescription.text = product.description.ifEmpty { "Sin descripción" }

        // CORRECCIÓN 2: Usamos 'image' en lugar de 'productImage'
        val imgUrl = product.image
        if (!imgUrl.isNullOrEmpty()) {
            Glide.with(this).load(imgUrl)
                .placeholder(R.drawable.ic_launcher_background) // Cambia por tu placeholder
                .into(ivProductImage)
        } else {
            ivProductImage.setImageResource(R.drawable.ic_launcher_background)
        }

        // Lógica del vendedor
        val vendor = product.vendor
        if (vendor != null) {
            // Usamos solo firstName porque en tu JSON no venía lastName
            tvVendorName.text = vendor.firstName ?: "Vendedor desconocido"
            tvVendorCareer.text = vendor.career ?: "Estudiante"

            if (!vendor.profileImage.isNullOrEmpty()) {
                Glide.with(this)
                    .load(vendor.profileImage)
                    .circleCrop()
                    .placeholder(R.drawable.ic_launcher_background)
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

    private fun addToCart(productId: Int, count: Int) {
        val sp = getSharedPreferences("cart_items", Context.MODE_PRIVATE)
        val current = sp.getString("cart_map", "") ?: ""
        val map = mutableMapOf<String, Int>()
        if (current.isNotEmpty()) {
            current.split(";").forEach {
                val parts = it.split(":")
                if (parts.size == 2) map[parts[0]] = parts[1].toInt()
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

        // Asegúrate de tener esta data class creada en Network
        val request = CreateReviewRequest(
            product = product.id,
            rating = rating,
            comment = comment
        )

        btnSubmitReview.isEnabled = false
        btnSubmitReview.text = "Enviando..."

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.postReview(request)
                if (response.isSuccessful) {
                    Toast.makeText(this@ProductDetailActivity, "¡Reseña enviada!", Toast.LENGTH_LONG).show()
                    etComment.text?.clear()
                    ratingBarInput.rating = 0f
                    loadReviews(product.id)
                } else {
                    Toast.makeText(this@ProductDetailActivity, "Error al enviar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProductDetailActivity, "Sin conexión", Toast.LENGTH_SHORT).show()
            } finally {
                btnSubmitReview.isEnabled = true
                btnSubmitReview.text = "Enviar reseña"
            }
        }
    }

    // ==================== EDITAR RESEÑA ====================
    private fun showEditDialog(review: ReviewModel) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_review, null)
        val etEditComment = dialogView.findViewById<EditText>(R.id.etEditComment)
        val ratingBarEdit = dialogView.findViewById<RatingBar>(R.id.ratingBarEdit)

        etEditComment.setText(review.comment)

        ratingBarEdit.post {
            ratingBarEdit.rating = review.rating.toFloat()
        }

        AlertDialog.Builder(this)
            .setTitle("Editar reseña")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val newComment = etEditComment.text.toString().trim()
                val newRating = ratingBarEdit.rating.toInt()

                if (newComment.isEmpty() || newRating == 0) {
                    Toast.makeText(this, "Completa todo", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                updateReview(review.id, newRating, newComment)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateReview(reviewId: Int, rating: Int, comment: String) {
        lifecycleScope.launch {
            try {
                val request = UpdateReviewRequest(rating = rating, comment = comment)
                val response = RetrofitClient.instance.updateReview(reviewId, request)
                if (response.isSuccessful) {
                    Toast.makeText(this@ProductDetailActivity, "Actualizado", Toast.LENGTH_SHORT).show()
                    loadReviews(product.id)
                } else {
                    Toast.makeText(this@ProductDetailActivity, "Error al actualizar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProductDetailActivity, "Sin conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteReview(reviewId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar")
            .setMessage("¿Borrar reseña?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.instance.deleteReview(reviewId)
                        if (response.isSuccessful) {
                            Toast.makeText(this@ProductDetailActivity, "Eliminado", Toast.LENGTH_SHORT).show()
                            loadReviews(product.id)
                        } else {
                            Toast.makeText(this@ProductDetailActivity, "Error", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@ProductDetailActivity, "Sin conexión", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}