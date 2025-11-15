package com.example.markettecnm.models

import java.io.Serializable

data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val rating: Float,
    val stock: Int,       // CAMBIO: De 'reviewsCount' a 'stock'
    val imageRes: Int,      // CAMBIO: De 'imageResId' a 'imageRes'
    var quantityInCart: Int = 1
) : Serializable