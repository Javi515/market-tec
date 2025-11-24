package com.example.markettecnm.network

import com.google.gson.annotations.SerializedName
import com.example.markettecnm.models.ProductModel

// ========== AUTENTICACIÓN ==========

data class LoginRequestBody(
    val username: String,
    val password: String
)

data class TokenResponse(
    @SerializedName("access")
    val accessToken: String,
    @SerializedName("refresh")
    val refreshToken: String? = null
)

data class RegistrationRequestBody(
    val first_name: String,
    val username: String,
    val email: String,
    val password: String,
    @SerializedName("password2")
    val password2: String,
    val control_number: String,
    val career: String,
    val phone_number: String,
    val date_of_birth: String
)

data class RegistrationResponse(
    val id: Int,
    val username: String,
    val email: String,
    val message: String? = null
)

data class ErrorResponse(
    val detail: String? = null,
    val email: List<String>? = null,
    val username: List<String>? = null,
    val password: List<String>? = null,
    @SerializedName("control_number")
    val controlNumber: List<String>? = null,
    @SerializedName("phone_number")
    val phoneNumber: List<String>? = null
)

// ========== VENDEDOR ==========

data class VendorModel(
    @SerializedName("first_name")
    val firstName: String? = null,
    @SerializedName("profile_image")
    val profileImage: String? = null,
    val career: String? = null
)

// ========== FAVORITOS ==========

data class FavoriteResponse(
    val id: Int,
    val product: ProductModel
)


data class ToggleFavoriteRequest(
    @SerializedName("product_id")
    val productId: Int
)

data class AddFavoriteRequest(
    @SerializedName("product_id")
    val productId: Int
)

// ========== RESEÑAS ==========

data class CreateReviewRequest(
    val product: Int,
    val rating: Int,
    val comment: String
)

data class UpdateReviewRequest(
    val rating: Int,
    val comment: String
)