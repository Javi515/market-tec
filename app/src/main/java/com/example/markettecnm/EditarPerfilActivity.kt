package com.example.markettecnm

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView // 👈 Importante
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts // 👈 Importante
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide // 👈 Para mostrar la imagen
import com.example.markettecnm.network.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Imports de modelos
import com.example.markettecnm.models.UserProfile
import com.example.markettecnm.models.UserProfileUpdate
import com.example.markettecnm.models.ProfileUpdateData

// Imports para subir imagen
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale

class EditarPerfilActivity : AppCompatActivity() {

    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhoneNumber: TextInputEditText
    private lateinit var etCareer: TextInputEditText
    private lateinit var etDateOfBirth: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnSaveProfile: Button

    // 👇 UI de Imagen
    private lateinit var ivProfileImage: ImageView
    private var selectedImageUri: Uri? = null

    private var currentUserId: Int = -1

    // 👇 Selector de Imagen de la Galería
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            // Mostrar visualmente la imagen seleccionada
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(ivProfileImage)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_perfil)

        supportActionBar?.title = "Editar Perfil"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findAllViews()
        setupListeners()
        loadProfileData()
    }

    private fun findAllViews() {
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etCareer = findViewById(R.id.etCareer)
        etDateOfBirth = findViewById(R.id.etDateOfBirth)
        etPassword = findViewById(R.id.etPassword)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        // 👇 Asegúrate de tener este ID en tu XML
        ivProfileImage = findViewById(R.id.ivProfileImage)
    }

    private fun setupListeners() {
        btnSaveProfile.setOnClickListener { attemptSaveProfile() }

        etDateOfBirth.setOnClickListener { showDatePicker() }
        etDateOfBirth.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDatePicker() }

        // 👇 Clic en la imagen abre la galería
        ivProfileImage.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    private fun loadProfileData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getMyProfile()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        bindData(response.body()!!)
                    } else {
                        Toast.makeText(this@EditarPerfilActivity, "Error al cargar perfil.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("PROFILE", "Error de red al cargar perfil.", e)
                    Toast.makeText(this@EditarPerfilActivity, "Error de red.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun bindData(profile: UserProfile) {
        currentUserId = profile.id

        etEmail.setText(profile.email)
        etFullName.setText(profile.firstName)

        profile.profile?.let { details ->
            etPhoneNumber.setText(details.phoneNumber)
            etCareer.setText(details.career)
            etDateOfBirth.setText(details.dateOfBirth)

            // 👇 Cargar la imagen actual del servidor
            if (!details.profileImage.isNullOrEmpty()) {
                Glide.with(this)
                    .load(details.profileImage)
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .circleCrop()
                    .into(ivProfileImage)
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val date = String.format(Locale.US, "%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            etDateOfBirth.setText(date)
        }, year, month, day)
        dpd.show()
    }

    private fun attemptSaveProfile() {
        if (currentUserId == -1) return

        val newFullName = etFullName.text.toString().trim()
        val newPhoneNumber = etPhoneNumber.text.toString().trim()
        val newCareer = etCareer.text.toString().trim()
        val newDateOfBirth = etDateOfBirth.text.toString().trim()
        val newPassword = etPassword.text.toString()

        if (newFullName.isEmpty() || newPhoneNumber.isEmpty() || newCareer.isEmpty() || newDateOfBirth.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.isNotEmpty() && newPassword.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show()
            return
        }

        btnSaveProfile.isEnabled = false
        btnSaveProfile.text = "Guardando..."

        // 1. Preparar datos de Texto (Anidados)
        val profileData = ProfileUpdateData(
            phoneNumber = newPhoneNumber,
            dateOfBirth = newDateOfBirth,
            career = newCareer
        )

        val updateRequest = UserProfileUpdate(
            firstName = newFullName,
            email = null,
            password = if (newPassword.isNotEmpty()) newPassword else null,
            profile = profileData
        )

        // 2. Iniciar secuencia de guardado
        saveProfileText(updateRequest)
    }

    // PASO 1: Guardar Texto
    private fun saveProfileText(updateRequest: UserProfileUpdate) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.updateProfile(updateRequest)

                if (response.isSuccessful) {
                    // Texto guardado OK. Verificamos si hay imagen.
                    if (selectedImageUri != null) {
                        withContext(Dispatchers.Main) {
                            btnSaveProfile.text = "Subiendo foto..."
                        }
                        uploadProfileImage() // Vamos al Paso 2
                    } else {
                        finishSuccess(updateRequest.firstName ?: "")
                    }
                } else {
                    handleError("Error guardando datos: ${response.code()}")
                }
            } catch (e: Exception) {
                handleError("Error de red al guardar datos.")
            }
        }
    }

    // PASO 2: Subir Imagen
    private fun uploadProfileImage() {
        try {
            val uri = selectedImageUri!!
            val file = uriToFile(uri)
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())

            // "profile.profile_image" apunta al campo anidado en Django
            val body = MultipartBody.Part.createFormData("profile.profile_image", file.name, requestFile)

            // Llamada síncrona dentro de la corrutina IO
            // Nota: Aquí se usa runBlocking implícito por estar en launch(IO)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.instance.updateProfileImage(body)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            finishSuccess(etFullName.text.toString())
                        } else {
                            Toast.makeText(this@EditarPerfilActivity, "Datos guardados, pero falló la imagen (${response.code()})", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    handleError("Error subiendo imagen.")
                }
            }

        } catch (e: Exception) {
            Log.e("UPLOAD", "Error preparando imagen", e)
            lifecycleScope.launch { handleError("Error al procesar imagen") }
        }
    }

    private suspend fun finishSuccess(firstName: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@EditarPerfilActivity, "¡Perfil actualizado correctamente!", Toast.LENGTH_SHORT).show()

            getSharedPreferences("markettec_prefs", Context.MODE_PRIVATE).edit()
                .putString("current_user_first_name", firstName).apply()

            finish()
        }
    }

    private suspend fun handleError(msg: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@EditarPerfilActivity, msg, Toast.LENGTH_LONG).show()
            btnSaveProfile.isEnabled = true
            btnSaveProfile.text = "Guardar Cambios"
        }
    }

    // Utilidad: Convierte Uri de galería a Archivo temporal
    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("profile_pic", ".jpg", cacheDir)
        inputStream?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }
}