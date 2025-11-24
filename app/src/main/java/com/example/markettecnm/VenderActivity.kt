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

class VenderActivity : AppCompatActivity() {

    // Views
    private lateinit var etProductName: TextInputEditText
    private lateinit var etProductDescription: TextInputEditText
    private lateinit var etProductPrice: TextInputEditText
    private lateinit var etProductInventory: TextInputEditText
    private lateinit var actvCategory: AutoCompleteTextView
    private lateinit var ivProductImage: ImageView
    private lateinit var tvSelectImage: TextView
    private lateinit var btnPublish: Button
    private lateinit var tvError: TextView

    // State
    private var selectedImageUri: Uri? = null
    private var availableCategories = listOf<String>()


    // Selector de Imagen (Abre galería)
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            // Mantener permiso de acceso
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            // Mostrar la imagen seleccionada
            ivProductImage.setImageURI(uri)
            tvSelectImage.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vender)

        supportActionBar?.title = "Publicar Venta"

        findAllViews()
        setupClickListeners()
        loadCategories()
    }

    private fun findAllViews() {
        etProductName = findViewById(R.id.etProductName)
        etProductDescription = findViewById(R.id.etProductDescription)
        etProductPrice = findViewById(R.id.etProductPrice)
        etProductInventory = findViewById(R.id.etProductInventory)
        actvCategory = findViewById(R.id.actvCategory)
        ivProductImage = findViewById(R.id.ivProductImage)
        tvSelectImage = findViewById(R.id.tvSelectImage)
        btnPublish = findViewById(R.id.btnPublish)
        tvError = findViewById(R.id.tvError)

        ivProductImage.clipToOutline = true
        tvSelectImage.setOnClickListener { ivProductImage.callOnClick() }
    }

    private fun setupClickListeners() {
        ivProductImage.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }

        btnPublish.setOnClickListener {
            attemptPublish()
        }
    }

    // Cargar Categorías para el Dropdown
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
                Log.e("VENDER", "Error cargando categorías: ${e.message}")
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

    // Lógica de Validación y Publicación
    private fun attemptPublish() {
        tvError.visibility = View.GONE

        val name = etProductName.text.toString().trim()
        val description = etProductDescription.text.toString().trim()
        val price = etProductPrice.text.toString().trim()
        val inventory = etProductInventory.text.toString().trim()
        val categoryName = actvCategory.text.toString().trim()

        if (name.isEmpty() || price.isEmpty() || inventory.isEmpty() || categoryName.isEmpty() || selectedImageUri == null) {
            tvError.text = "Completa todos los campos y selecciona una imagen."
            tvError.visibility = View.VISIBLE
            return
        }

        btnPublish.isEnabled = false
        btnPublish.text = "Publicando..."

        publishProduct(name, description, price, inventory, categoryName)
    }

    private fun publishProduct(name: String, description: String, price: String, inventory: String, categoryName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            var imagePart: MultipartBody.Part? = null

            // 1. Procesar la imagen
            selectedImageUri?.let { uri ->
                try {
                    val file = uriToFile(uri, name)
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    imagePart = MultipartBody.Part.createFormData("product_image", file.name, requestFile)
                } catch (e: Exception) {
                    Log.e("VENDER", "Error procesando imagen: ${e.message}")
                    withContext(Dispatchers.Main) {
                        showError("Error al procesar la imagen.")
                    }
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

            // 3. Llamada a la API
            try {
                val response = RetrofitClient.instance.createProduct(params, imagePart)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@VenderActivity, "¡Producto publicado con éxito!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("VENDER", "Error API: ${response.code()} | Body: $errorBody")
                        showError("Error al publicar. Código: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Error de red: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    btnPublish.isEnabled = true
                    btnPublish.text = "Publicar Producto"
                }
            }
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
        btnPublish.isEnabled = true
        btnPublish.text = "Publicar Producto"
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