package com.example.markettecnm.models // (O el package que tengas configurado)

import com.google.gson.annotations.SerializedName
import com.example.markettecnm.network.VendorModel // Asegúrate de que este import exista o que VendorModel esté en este archivo



// --- MODIFICADO: Tu ProductModel ---
data class ProductModel(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("price") val price: String,
    @SerializedName("inventory") val inventory: Int,
    @SerializedName("status") val status: String,
    @SerializedName("vendor") val vendor: VendorModel, // Asegúrate de tener esta clase definida
    @SerializedName("category_name") val categoryName: String,

    // CAMBIO IMPORTANTE:
    // En el JSON viene como "product_image", pero en Kotlin lo llamaremos "image"
    // para que sea compatible con el código de tus adaptadores.
    @SerializedName("product_image") val image: String? = null
)

