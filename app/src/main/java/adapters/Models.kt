package com.example.markettecnm.network

import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos para un producto, exactamente como lo devuelve tu API:
 * http://172.200.235.24/api/products/
 */
data class ProductModel(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("price") val price: String,
    @SerializedName("inventory") val inventory: Int,
    @SerializedName("status") val status: String,
    @SerializedName("vendor") val vendor: VendorModel,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("product_image") val productImage: String? = null
)