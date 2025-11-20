package com.example.markettecnm.models

import com.google.gson.annotations.SerializedName

data class ReviewRequest(
    @SerializedName("reviewer") val reviewer: Reviewer = Reviewer(
        firstName = "Anónimo",
        profileImage = null,
        career = null
    ),
    @SerializedName("product") val product: Int,
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String
)