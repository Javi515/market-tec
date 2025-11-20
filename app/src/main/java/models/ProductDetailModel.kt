package com.example.markettecnm.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable  // ← Import correcto

/**
 * Modelo detallado del producto para la pantalla de detalle
 */
data class ProductDetailModel(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("price") val price: String,
    @SerializedName("inventory") val inventory: Int,
    @SerializedName("status") val status: String,
    @SerializedName("vendor") val vendor: Vendor?,  // ← Apunta a la clase Vendor de abajo
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("product_image") val productImage: String? = null
)

/**
 * Modelo del vendedor con nombre + apellido
 * Solo debe existir UNA sola vez en todo el proyecto
 */
