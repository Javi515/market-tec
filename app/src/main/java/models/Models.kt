package com.example.markettecnm.models

import com.google.gson.annotations.SerializedName
// Mantenemos el import de VendorModel, ya que su definición está en el paquete network
import com.example.markettecnm.network.VendorModel

// ======================================================================
// 1. PRODUCT MODEL (Objeto principal de la tienda)
// ======================================================================
data class ProductModel(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("price") val price: String,
    @SerializedName("inventory") val inventory: Int,
    @SerializedName("status") val status: String,

    // VendorModel es un objeto anidado cuyo DTO vive en el paquete network
    @SerializedName("vendor") val vendor: VendorModel,

    @SerializedName("category_name") val categoryName: String,

    // El campo de imagen que usamos en Glide (mapeado desde product_image)
    @SerializedName("product_image") val image: String? = null
)

// ======================================================================
// 2. RESEÑAS Y REVIEWER (Necesario para ProductDetailActivity)
// ======================================================================

data class ReviewModel(
    val id: Int,

    // Este campo es crucial para la lógica de Tendencias y Filtro en Detalle
    @SerializedName("product")
    val product: Int,

    val rating: Int,
    val comment: String?,

    @SerializedName("created_at")
    val createdAt: String? = null,

    // El objeto Reviewer anidado
    val reviewer: Reviewer? = null
)

// Objeto anidado que describe al usuario que hizo la reseña
data class Reviewer(
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("profile_image") val profileImage: String?,
    val career: String?
)