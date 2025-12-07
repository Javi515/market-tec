package com.example.markettecnm.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Modelo de datos para la información del vendedor.
 * IMPORTANTE: La clase se llama VendorModel para coincidir con ProductModel.
 */
/*data class VendorModel(
    // 👇 VITAL: ID del vendedor con valor por defecto 0
    // Esto permite que la app lea el ID si viene, o use 0 si no viene, sin crashear.
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("first_name")
    val firstName: String?,

    @SerializedName("last_name")
    val lastName: String?,

    @SerializedName("profile_image")
    val profileImage: String?,

    @SerializedName("career")
    val career: String?
) : Serializable*/