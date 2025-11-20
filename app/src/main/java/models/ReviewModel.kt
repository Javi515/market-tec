package com.example.markettecnm.models

import com.google.gson.annotations.SerializedName

data class ReviewModel(
    @SerializedName("id") val id: Int,
    @SerializedName("reviewer") val reviewer: Reviewer,
    @SerializedName("product") val product: Int,
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String,
    @SerializedName("created_at") val createdAt: String
)

data class Reviewer(
    @SerializedName("first_name") val firstName: String,
    @SerializedName("profile_image") val profileImage: String? = null,
    @SerializedName("career") val career: String? = null
)