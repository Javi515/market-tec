package com.example.markettecnm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.markettecnm.network.ErrorResponse
import com.example.markettecnm.network.RegistrationRequestBody
import com.example.markettecnm.network.RetrofitClient // <-- ¡IMPORT AGREGADO!
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class RegistroActivity : AppCompatActivity() {

    // 1. Declaración de Vistas
    private lateinit var etNombreCompleto: TextInputEditText
    private lateinit var etNombreUsuario: TextInputEditText
    private lateinit var etCorreoInstitucional: TextInputEditText
    private lateinit var etContrasena: TextInputEditText
    private lateinit var etConfirmarContrasena: TextInputEditText
    private lateinit var etCarrera: TextInputEditText
    private lateinit var etFechaNacimiento: TextInputEditText
    private lateinit var etNumeroControl: TextInputEditText
    private lateinit var etNumeroTelefono: TextInputEditText
    private lateinit var btnCrearCuenta: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // 2. Inicialización de Vistas
        etNombreCompleto = findViewById(R.id.etNombreCompleto)
        etNombreUsuario = findViewById(R.id.etNombreUsuario)
        etCorreoInstitucional = findViewById(R.id.etCorreoInstitucional)
        etContrasena = findViewById(R.id.etContrasena)
        etConfirmarContrasena = findViewById(R.id.etConfirmarContrasena)
        etCarrera = findViewById(R.id.etCarrera)
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento)
        etNumeroControl = findViewById(R.id.etNumeroControl)
        etNumeroTelefono = findViewById(R.id.etNumeroTelefono)
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta)

        // 3. Configuración del Listener
        btnCrearCuenta.setOnClickListener {
            attemptRegistration()
        }
    }

    /**
     * Captura los datos del formulario, realiza la validación y la llamada a la API.
     */
    private fun attemptRegistration() {
        // 4. Captura de datos
        val firstName = etNombreCompleto.text.toString().trim()
        val username = etNombreUsuario.text.toString().trim()
        val email = etCorreoInstitucional.text.toString().trim()
        val password = etContrasena.text.toString()
        val password2 = etConfirmarContrasena.text.toString()
        val career = etCarrera.text.toString().trim()
        val dateOfBirthInput = etFechaNacimiento.text.toString().trim()
        val controlNumber = etNumeroControl.text.toString().trim()
        val phoneNumber = etNumeroTelefono.text.toString().trim()

        // 5. Validación completa de campos requeridos y contraseñas
        if (password != password2) {
            Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_LONG).show()
            return
        }
        if (controlNumber.isEmpty() || phoneNumber.isEmpty() || firstName.isEmpty() || username.isEmpty() || email.isEmpty() || career.isEmpty() || dateOfBirthInput.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos requeridos.", Toast.LENGTH_LONG).show()
            return
        }

        // Reformatear la fecha (DD/MM/AAAA -> YYYY-MM-DD)
        val formattedDate = formatDate(dateOfBirthInput)
        if (formattedDate == null) {
            Toast.makeText(this, "Formato de fecha de nacimiento incorrecto (DD/MM/AAAA).", Toast.LENGTH_LONG).show()
            return
        }

        // 6. Creación del cuerpo de la solicitud
        val requestBody = RegistrationRequestBody(
            first_name = firstName,
            username = username,
            email = email,
            password = password,
            password2 = password2,
            control_number = controlNumber,
            phone_number = phoneNumber,
            career = career,
            date_of_birth = formattedDate
        )

        // 7. Lanzamiento de la Coroutine para la llamada a la API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ahora RetrofitClient es reconocido gracias al import
                val response = RetrofitClient.instance.registerUser(requestBody)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Éxito: Mostrar Dialog y Redirigir
                        showSuccessDialog()
                    } else {
                        // Error del servidor/cliente
                        handleApiError(response.errorBody()?.string(), response.code())
                    }
                }
            } catch (e: Exception) {
                // Error de red
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegistroActivity, "Error de red. Verifica la conexión: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("NETWORK_ERROR", "Excepción de red: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Muestra un diálogo de éxito y navega a la actividad de inicio de sesión (MainActivity).
     */
    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("¡Registro Exitoso!")
            .setMessage("Tu cuenta ha sido creada. Serás redirigido al inicio de sesión.")
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
                // Redirige a la actividad de inicio de sesión (MainActivity)
                val intent = Intent(this, MainActivity::class.java).apply {
                    // Limpia la pila de actividades para evitar volver al registro con el botón Atrás
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
            .setCancelable(false) // No se puede cancelar tocando fuera
            .show()
    }

    /**
     * Procesa la respuesta de error de la API y muestra un mensaje relevante.
     */
    private fun handleApiError(errorBodyString: String?, code: Int) {
        try {
            val errorResponse = Gson().fromJson(errorBodyString, ErrorResponse::class.java)

            val errorMessage = when {
                errorResponse?.detail != null -> errorResponse.detail
                errorResponse?.email?.isNotEmpty() == true -> "Error de email: ${errorResponse.email.first()}"
                errorResponse?.username?.isNotEmpty() == true -> "Error de usuario: ${errorResponse.username.first()}"
                errorResponse?.password?.isNotEmpty() == true -> "Error de contraseña: ${errorResponse.password.first()}"
                errorResponse?.controlNumber?.isNotEmpty() == true -> "Error C. Control: ${errorResponse.controlNumber.first()}"
                errorResponse?.phoneNumber?.isNotEmpty() == true -> "Error Teléfono: ${errorResponse.phoneNumber.first()}"
                else -> "Error de registro: Código $code. Intenta nuevamente."
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            Log.e("API_ERROR", "Respuesta de error completa: $errorBodyString")

        } catch (e: Exception) {
            Toast.makeText(this, "Error desconocido en el servidor: Código $code", Toast.LENGTH_LONG).show()
            Log.e("API_ERROR", "Fallo al parsear JSON de error: ${e.message}")
        }
    }

    /**
     * Convierte la fecha de "DD/MM/AAAA" a "AAAA-MM-DD" para el API.
     */
    private fun formatDate(dateString: String): String? {
        return try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            null
        }
    }
}