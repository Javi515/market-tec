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
import com.example.markettecnm.network.CategoryModel // Necesario para obtener el ID
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
    // [1] State 1: Lista de nombres para el Dropdown
    private var availableCategoryNames = listOf<String>()
    // [2] State 2: Mapa para obtener el ID al momento de publicar
    private var categoryIdMap: Map<String, Int> = emptyMap()


    // Selector de Imagen (pickImage)
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            ivProductImage.scaleType = ImageView.ScaleType.CENTER_CROP
            ivProductImage.setImageURI(uri)
            tvSelectImage.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 💡 CORRECCIÓN: Usar el nombre de layout correcto
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
            ivProductImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
            pickImage.launch(arrayOf("image/*"))
        }

        btnPublish.setOnClickListener {
            attemptPublish()
        }
    }

    // 🛑 CORRECCIÓN: Cargar y Mapear ID de Categoría
    private fun loadCategories() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getCategories()
                if (response.isSuccessful) {
                    val categories = response.body() ?: emptyList<CategoryModel>()

                    // Almacenamos un mapa Nombre -> ID
                    categoryIdMap = categories.associate { it.name to it.id }
                    // Y la lista de nombres para el adaptador del dropdown
                    availableCategoryNames = categories.map { it.name }

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
            availableCategoryNames // Usamos solo la lista de nombres
        )
        actvCategory.setAdapter(adapter)
    }

    // Lógica de Validación y Publicación
    private fun attemptPublish() {
        tvError.visibility = View.GONE

        val name = etProductName.text.toString().trim()
        val price = etProductPrice.text.toString().trim()
        val inventory = etProductInventory.text.toString().trim()
        val categoryName = actvCategory.text.toString().trim()

        // 💡 OBTENEMOS EL ID DE LA CATEGORÍA A PARTIR DEL NOMBRE
        val categoryId = categoryIdMap[categoryName]

        if (name.isEmpty() || price.isEmpty() || inventory.isEmpty() || categoryId == null || selectedImageUri == null) {
            tvError.text = "Completa todos los campos y selecciona una imagen."
            if (categoryId == null && categoryName.isNotEmpty()) {
                tvError.text = "Selecciona una categoría válida del menú desplegable."
            }
            tvError.visibility = View.VISIBLE
            return
        }

        btnPublish.isEnabled = false
        btnPublish.text = "Publicando..."

        publishProduct(name, etProductDescription.text.toString().trim(), price, inventory, categoryId)
    }

    // 🛑 CORRECCIÓN CLAVE: El DTO de publicación ahora recibe el ID (Int)
    private fun publishProduct(name: String, description: String, price: String, inventory: String, categoryId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            var imagePart: MultipartBody.Part? = null

            // 1. Procesar la imagen
            selectedImageUri?.let { uri ->
                try {
                    val file = uriToFile(uri, name)
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    imagePart = MultipartBody.Part.createFormData("product_image", file.name, requestFile)
                } catch (e: Exception) {
                    // ... (manejo de error) ...
                    return@launch
                }
            }

            // 2. Crear los RequestBody para los campos de texto
            val params = mutableMapOf<String, RequestBody>()
            params["name"] = name.toRequestBody("text/plain".toMediaTypeOrNull())
            params["description"] = description.toRequestBody("text/plain".toMediaTypeOrNull())
            params["price"] = price.toRequestBody("text/plain".toMediaTypeOrNull())
            params["inventory"] = inventory.toRequestBody("text/plain".toMediaTypeOrNull())
            // 🛑 CAMBIO CLAVE: Enviamos el ID bajo la clave "category"
            params["category"] = categoryId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

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
        btnPublish.isEnabled = true
        btnPublish.text = "Publicar Producto"
        tvError.visibility = View.VISIBLE
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