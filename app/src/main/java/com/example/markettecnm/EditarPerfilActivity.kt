package com.example.markettecnm

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.content.Context
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.markettecnm.network.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 👇 FIX: Imports necesarios para los DTOs de Perfil
import com.example.markettecnm.network.UserProfile
import com.example.markettecnm.network.UserProfileUpdate
import com.example.markettecnm.network.ProfileDetail
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

    private var currentUserId: Int = -1

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

        // Bloquear edición de correo
        etEmail.isEnabled = false
    }

    private fun setupListeners() {
        btnSaveProfile.setOnClickListener { attemptSaveProfile() }

        // Selector de Fecha
        etDateOfBirth.setOnClickListener { showDatePicker() }
        etDateOfBirth.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDatePicker() }
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
        currentUserId = profile.id // Guardar ID

        // FIX: El email ahora se resuelve gracias a la importación y definición en DataModels.kt
        etEmail.setText(profile.email)
        etFullName.setText(profile.firstName)

        // FIX: Acceso al campo anidado 'profile' (donde está el teléfono/carrera/fecha)
        profile.profile?.let { details ->
            etPhoneNumber.setText(details.phoneNumber)
            etCareer.setText(details.career)
            etDateOfBirth.setText(details.dateOfBirth)
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

        val updateRequest = UserProfileUpdate(
            firstName = newFullName,
            phoneNumber = newPhoneNumber,
            dateOfBirth = newDateOfBirth,
            password = if (newPassword.isNotEmpty()) newPassword else null,
            email = null, // Email no se actualiza por aquí
        )

        saveProfile(updateRequest)
    }

    private fun saveProfile(updateRequest: UserProfileUpdate) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.updateProfile(updateRequest)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EditarPerfilActivity, "Perfil actualizado con éxito!", Toast.LENGTH_LONG).show()
                        // FIX: Actualizar el nombre guardado localmente después de un cambio exitoso
                        getSharedPreferences("markettec_prefs", Context.MODE_PRIVATE).edit()
                            .putString("current_user_first_name", updateRequest.firstName).apply()

                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("PROFILE_SAVE", "Fallo: ${response.code()} Body: $errorBody")
                        Toast.makeText(this@EditarPerfilActivity, "Error ${response.code()} al guardar.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("PROFILE_SAVE", "Error de red.", e)
                    Toast.makeText(this@EditarPerfilActivity, "Error de red.", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    btnSaveProfile.isEnabled = true
                    btnSaveProfile.text = "Guardar Cambios"
                }
            }
        }
    }
}