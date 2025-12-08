package com.example.markettecnm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CuentaFragment : Fragment() {

    private lateinit var imgAvatar: ImageView
    private lateinit var fabChangePhoto: FloatingActionButton
    private lateinit var textNombre: TextView
    private lateinit var textCorreo: TextView
    private lateinit var btnEditarPerfil: Button

    private val sessionPrefs by lazy {
        requireContext().getSharedPreferences("markettec_prefs", Context.MODE_PRIVATE)
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

        // 2. Configurar Botones
        setupListeners(view)
    }

    override fun onResume() {
        super.onResume()
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getMyProfile()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val userProfile = response.body()!!

                        // 1. Actualizar Textos
                        textNombre.text = userProfile.firstName ?: "Usuario"
                        textCorreo.text = userProfile.email

                        // 2. Actualizar Foto con Glide (Desde el servidor)
                        val imageUrl = userProfile.profile?.profileImage

                        if (!imageUrl.isNullOrEmpty()) {
                            // ⚠️ NOTA: Si el link de la foto es local (ej. 172.x.x.x) y no carga,
                            // debes asegurarte de que Glide pueda acceder a la IP.
                            Glide.with(this@CuentaFragment)
                                .load(imageUrl)
                                .placeholder(android.R.drawable.ic_menu_camera)
                                .error(android.R.drawable.ic_menu_camera)
                                .circleCrop()
                                .into(imgAvatar)
                        } else {
                            imgAvatar.setImageResource(android.R.drawable.ic_menu_camera)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CUENTA", "Error al cargar perfil", e)
            }
        }
    }

    private fun setupListeners(view: View) {
        // Botones de Edición
        fabChangePhoto.setOnClickListener {
            startActivity(Intent(requireContext(), EditarPerfilActivity::class.java))
        }

        btnEditarPerfil.setOnClickListener {
            startActivity(Intent(requireContext(), EditarPerfilActivity::class.java))
        }

        // ======= Apartados interactivos de la lista =======
        view.findViewById<View>(R.id.rowCompras).setOnClickListener {
            startActivity(Intent(requireContext(), MisComprasActivity::class.java))
        }

        view.findViewById<View>(R.id.rowMisVentas).setOnClickListener {
            startActivity(Intent(requireContext(), MisVentasActivity::class.java))
        }

        // 🟢 CORRECCIÓN CLAVE: Abrir ChatListActivity, que es la lista de chats
        view.findViewById<View>(R.id.rowChats).setOnClickListener {
            startActivity(Intent(requireContext(), ChatListActivity::class.java)) // <--- APUNTA A LA LISTA
        }

        view.findViewById<View>(R.id.rowVender).setOnClickListener {
            startActivity(Intent(requireContext(), VenderActivity::class.java))
        }

        view.findViewById<View>(R.id.rowPublicaciones).setOnClickListener {
            startActivity(Intent(requireContext(), PublicacionesActivity::class.java))
        }

        view.findViewById<View>(R.id.rowLogout).setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        // Limpiar datos de sesión
        sessionPrefs.edit().clear().apply()

        // Limpiar caché de imágenes de Glide (Recomendado)
        Thread { Glide.get(requireContext()).clearDiskCache() }.start()
        Glide.get(requireContext()).clearMemory()

        // Regresar al login
        val i = Intent(requireContext(), MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
    }
}