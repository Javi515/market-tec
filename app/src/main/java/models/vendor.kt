package com.example.markettecnm.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Modelo de datos para la información del vendedor (Vendor) asociado a un producto.
 * Ahora incluye nombre + apellido para mostrar el nombre completo.
 */
data class Vendor(
    // Nombre del vendedor → viene como "first_name" en el JSON
    @SerializedName("first_name")
    val firstName: String?,

    // Apellido del vendedor → viene como "last_name" en el JSON
    @SerializedName("last_name")
    val lastName: String?,

    // URL de la foto de perfil → viene como "profile_image"
    @SerializedName("profile_image")
    val profileImage: String?,

    // Carrera del estudiante → viene como "career"
    @SerializedName("career")
    val career: String?
) : Serializable