package com.example.markettecnm.network

data class AddFavoriteRequest(
    val product: Int
)

data class FavoriteResponse(
    val id: Int,
    val product: ProductModel  // ← Aquí va el objeto completo del producto
)