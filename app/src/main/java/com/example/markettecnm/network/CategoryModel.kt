package com.example.markettecnm.network

data class CategoryModel(
    val id: Int,
    val name: String,
    val description: String?, // Puede ser null según tu API
    val image: String?        // <--- ESTO ES LO QUE TE FALTA
)