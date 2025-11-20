package com.example.markettecnm.models

data class CreateReviewRequest(
    val product: Int,
    val rating: Int,
    val comment: String
)