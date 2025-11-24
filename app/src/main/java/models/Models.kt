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

    // 💡 CORRECCIÓN CLAVE: Cambiado de Int a String para soportar números grandes (BigInt)
    // y evitar fallos en la deserialización (Gson) durante la edición.
    @SerializedName("inventory") val inventory: String,

    @SerializedName("status") val status: String,

    @SerializedName("vendor") val vendor: VendorModel,

    @SerializedName("category_name") val categoryName: String,

    @SerializedName("product_image") val image: String? = null
)

// ======================================================================
// 2. RESEÑAS Y REVIEWER (Necesario para ProductDetailActivity)
// ======================================================================

data class ReviewModel(
    val id: Int,

    @SerializedName("product")
    val product: Int,

    val rating: Int,
    val comment: String?,

    @SerializedName("created_at")
    val createdAt: String? = null,

    val reviewer: Reviewer? = null
)

// Objeto anidado que describe al usuario que hizo la reseña
data class Reviewer(
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("profile_image") val profileImage: String?,
    val career: String?
)