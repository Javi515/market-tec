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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class CuentaFragment : Fragment() {

    private lateinit var imgAvatar: ImageView
    private lateinit var btnCambiarFoto: Button

    // Preferencias locales para guardar la URI de la imagen
    private val prefs by lazy {
        requireContext().getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
    }

    // Abrir galería para seleccionar imagen
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Mantener permiso de acceso a la imagen
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            // Guardar y mostrar la imagen
            saveAvatarUri(uri)
            imgAvatar.setImageURI(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_cuenta, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgAvatar = view.findViewById(R.id.imgAvatar)
        btnCambiarFoto = view.findViewById(R.id.btnCambiarFoto)

        // Cargar imagen guardada si existe
        loadSavedAvatar()?.let { uri ->
            imgAvatar.setImageURI(uri)
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
        }

        // Botón para abrir galería
        btnCambiarFoto.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }

        // ======= Apartados interactivos =======
        view.findViewById<View>(R.id.rowCompras).setOnClickListener {
            startActivity(Intent(requireContext(), MisComprasActivity::class.java))
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

        view.findViewById<View>(R.id.rowAccesibilidad).setOnClickListener {
            startActivity(Intent(requireContext(), AccesibilidadActivity::class.java))
        }

        view.findViewById<View>(R.id.rowLogout).setOnClickListener {
            // Limpiar datos de sesión
            val sessionPrefs = requireContext().getSharedPreferences("markettec_prefs", Context.MODE_PRIVATE)
            sessionPrefs.edit().clear().apply()
            prefs.edit().clear().apply()

            // Regresar al login
            val i = Intent(requireContext(), MainActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }
    }

    // Guardar URI en SharedPreferences
    private fun saveAvatarUri(uri: Uri) {
        prefs.edit().putString("avatar_uri", uri.toString()).apply()
    }

    // Leer URI guardada
    private fun loadSavedAvatar(): Uri? {
        val s = prefs.getString("avatar_uri", null) ?: return null
        return Uri.parse(s)
    }
}
