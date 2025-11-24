package com.example.markettecnm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CuentaFragment : Fragment() {

    private lateinit var imgAvatar: ImageView
    private lateinit var fabChangePhoto: FloatingActionButton

    private lateinit var textNombre: TextView
    private lateinit var textCorreo: TextView
    private lateinit var btnEditarPerfil: Button

    private val sessionPrefs by lazy {
        requireContext().getSharedPreferences("markettec_prefs", Context.MODE_PRIVATE)
    }

    private val avatarPrefs by lazy {
        requireContext().getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            saveAvatarUri(uri)
            imgAvatar.setImageURI(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_cuenta, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicialización de Vistas
        imgAvatar = view.findViewById(R.id.imgAvatar)
        fabChangePhoto = view.findViewById(R.id.fabChangePhoto)

        textNombre = view.findViewById(R.id.textNombre)
        textCorreo = view.findViewById(R.id.textCorreo)
        btnEditarPerfil = view.findViewById(R.id.btnEditarPerfil)

        // 2. Cargar la imagen del avatar guardada (si existe)
        loadSavedAvatar()?.let { uri ->
            imgAvatar.setImageURI(uri)
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
        }

        // 3. Mostrar nombre y correo de la sesión actual
        displayUserInfo()

        // 4. Listeners

        fabChangePhoto.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }

        btnEditarPerfil.setOnClickListener {
            // FIX: Usamos el nombre simple para evitar el error de ruta si el import falla.
            val intent = Intent(requireContext(), EditarPerfilActivity::class.java)
            startActivity(intent)
        }

        // ======= Apartados interactivos de la lista =======
        view.findViewById<View>(R.id.rowCompras).setOnClickListener {
            startActivity(Intent(requireContext(), MisComprasActivity::class.java))
        }

        // 🛑 CORRECCIÓN CLAVE: AGREGAR EL LISTENER DE MIS VENTAS
        view.findViewById<View>(R.id.rowMisVentas).setOnClickListener {
            startActivity(Intent(requireContext(), MisVentasActivity::class.java))
        }

        view.findViewById<View>(R.id.rowChats).setOnClickListener {
            startActivity(Intent(requireContext(), HistorialActivity::class.java))
        }

        view.findViewById<View>(R.id.rowVender).setOnClickListener {
            startActivity(Intent(requireContext(), VenderActivity::class.java))
        }

        view.findViewById<View>(R.id.rowPublicaciones).setOnClickListener {
            startActivity(Intent(requireContext(), PublicacionesActivity::class.java))
        }

        view.findViewById<View>(R.id.rowLogout).setOnClickListener {
            // Limpiar datos de sesión y avatar
            sessionPrefs.edit().clear().apply()
            avatarPrefs.edit().clear().apply()

            // Regresar al login
            val i = Intent(requireContext(), MainActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }
    }

    // Función para leer y mostrar los datos del perfil
    private fun displayUserInfo() {
        val name = sessionPrefs.getString("current_user_first_name", "Invitado")
        val loginId = sessionPrefs.getString("username", "Sin sesión")

        textNombre.text = name
        textCorreo.text = loginId
    }

    // Funciones auxiliares
    private fun saveAvatarUri(uri: Uri) {
        avatarPrefs.edit().putString("avatar_uri", uri.toString()).apply()
    }

    private fun loadSavedAvatar(): Uri? {
        val s = avatarPrefs.getString("avatar_uri", null) ?: return null
        return Uri.parse(s)
    }
}