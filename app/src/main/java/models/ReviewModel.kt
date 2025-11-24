package com.example.markettecnm.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo completo para las Reseñas.
 * Incluye la clase 'Reviewer' para los datos del usuario que comenta.
 */
data class ReviewModel(
    val id: Int,

    // Este campo es VITAL para tu carrusel de tendencias (vincula con el producto)
    @SerializedName("product")
    val product: Int,

    val rating: Int,
    val comment: String?,

    @SerializedName("created_at")
    val createdAt: String? = null,

    // Objeto anidado del usuario que hizo la reseña
    val reviewer: Reviewer? = null
)

data class Reviewer(
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("profile_image") val profileImage: String?,
    val career: String?
)