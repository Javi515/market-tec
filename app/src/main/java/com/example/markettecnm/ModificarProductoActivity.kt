package com.example.markettecnm

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.network.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ModificarProductoActivity : AppCompatActivity() {

    private var productId: Int = -1
    private var originalImageUrl: String? = null
    private var selectedImageUri: Uri? = null
    private var availableCategories = listOf<String>()

    private lateinit var etProductName: TextInputEditText
    private lateinit var etProductDescription: TextInputEditText
    private lateinit var etProductPrice: TextInputEditText
    private lateinit var etProductInventory: TextInputEditText
    private lateinit var actvCategory: AutoCompleteTextView
    private lateinit var ivProductImage: ImageView
    private lateinit var tvSelectImage: TextView
    private lateinit var btnSave: Button
    private lateinit var tvError: TextView

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ivProductImage.setImageURI(uri)
            tvSelectImage.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // [1] Carga el layout de edición
        setContentView(R.layout.activity_modificar_producto)

        // [2] Lee el ID y verifica que no sea -1
        productId = intent.getIntExtra("PRODUCT_TO_EDIT_ID", -1)
        if (productId == -1) {
            Toast.makeText(this, "Error: Producto no especificado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        findAllViews()
        loadCategories()
        loadProductData(productId)
        setupClickListeners()
    }

    private fun findAllViews() {
        etProductName = findViewById(R.id.etProductName)
        etProductDescription = findViewById(R.id.etProductDescription)
        etProductPrice = findViewById(R.id.etProductPrice)
        etProductInventory = findViewById(R.id.etProductInventory)
        actvCategory = findViewById(R.id.actvCategory)
        ivProductImage = findViewById(R.id.ivProductImage)
        tvSelectImage = findViewById(R.id.tvSelectImage)
        btnSave = findViewById(R.id.btnSave)
        tvError = findViewById(R.id.tvError)

        ivProductImage.clipToOutline = true
        tvSelectImage.setOnClickListener { ivProductImage.callOnClick() }
    }

    private fun setupClickListeners() {
        ivProductImage.setOnClickListener { pickImage.launch(arrayOf("image/*")) }
        btnSave.setOnClickListener { attemptSave() }
    }

    // 3. Carga Datos Existentes del Producto
    private fun loadProductData(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getProductDetail(id)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val product = response.body()!!
                        bindExistingData(product)
                    } else {
                        Log.e("EDIT_LOAD", "Fallo al cargar: Código ${response.code()}")
                        Toast.makeText(this@ModificarProductoActivity, "No se pudo cargar el producto.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ModificarProductoActivity, "Error de red al cargar datos.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    // 4. Rellenar los Campos con Datos Existentes
    private fun bindExistingData(product: ProductModel) {
        etProductName.setText(product.name)
        etProductDescription.setText(product.description)
        etProductPrice.setText(product.price)
        etProductInventory.setText(product.inventory.toString()) // Asumimos Long/String y convertimos
        actvCategory.setText(product.categoryName, false)

        originalImageUrl = product.image

        if (!product.image.isNullOrEmpty()) {
            Glide.with(this)
                .load(product.image)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(ivProductImage)
            tvSelectImage.visibility = View.GONE
        } else {
            tvSelectImage.visibility = View.VISIBLE
        }
    }

    // 5. Guardar Cambios
    private fun attemptSave() {
        tvError.visibility = View.GONE
        val name = etProductName.text.toString().trim()
        val description = etProductDescription.text.toString().trim()
        val price = etProductPrice.text.toString().trim()
        val inventory = etProductInventory.text.toString().trim()
        val categoryName = actvCategory.text.toString().trim()

        if (name.isEmpty() || price.isEmpty() || inventory.isEmpty() || categoryName.isEmpty()) {
            tvError.text = "Completa todos los campos."
            tvError.visibility = View.VISIBLE
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Guardando..."

        saveChanges(name, description, price, inventory, categoryName)
    }

    private fun saveChanges(name: String, description: String, price: String, inventory: String, categoryName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            var imagePart: MultipartBody.Part? = null

            // 1. PROCESAR IMAGEN: Solo si el usuario seleccionó una NUEVA
            if (selectedImageUri != null) {
                try {
                    val file = uriToFile(selectedImageUri!!, name)
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    imagePart = MultipartBody.Part.createFormData("product_image", file.name, requestFile)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { showError("Error al procesar la nueva imagen.") }
                    return@launch
                }
            }

            // 2. Crear los RequestBody para los campos de texto
            val params = mutableMapOf<String, RequestBody>()
            params["name"] = name.toRequestBody("text/plain".toMediaTypeOrNull())
            params["description"] = description.toRequestBody("text/plain".toMediaTypeOrNull())
            params["price"] = price.toRequestBody("text/plain".toMediaTypeOrNull())
            params["inventory"] = inventory.toRequestBody("text/plain".toMediaTypeOrNull())
            params["category_name"] = categoryName.toRequestBody("text/plain".toMediaTypeOrNull())

            // 3. Llamada a la API (PATCH)
            try {
                // LLAMADA PATCH (ACTUALIZAR) CON EL ID CORRECTO
                val response = RetrofitClient.instance.updateProduct(productId, params, imagePart)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ModificarProductoActivity, "Cambios guardados con éxito!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("EDITAR_API", "Error al guardar. Código: ${response.code()} | Body: $errorBody")
                        showError("Error al guardar. Código: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("Error de red al guardar: ${e.message}") }
            } finally {
                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true
                    btnSave.text = "Guardar Cambios"
                }
            }
        }
    }

    // --- Funciones Auxiliares (Compartidas con VenderActivity) ---

    private fun loadCategories() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getCategories()
                if (response.isSuccessful) {
                    availableCategories = response.body()?.map { it.name } ?: emptyList()
                    withContext(Dispatchers.Main) {
                        setupCategoryDropdown()
                    }
                }
            } catch (e: Exception) {
                Log.e("EDITAR", "Error cargando categorías: ${e.message}")
            }
        }
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            availableCategories
        )
        actvCategory.setAdapter(adapter)
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
        btnSave.isEnabled = true
        btnSave.text = "Guardar Cambios"
    }

    private fun uriToFile(uri: Uri, fileName: String): File {
        val contentResolver = applicationContext.contentResolver
        val tempFile = File(cacheDir, "$fileName.jpg")

        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return tempFile
    }
}