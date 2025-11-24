package com.example.markettecnm.network

import com.google.gson.annotations.SerializedName
import com.example.markettecnm.models.ProductModel

// ======================================================================
// DTOs DE AUTENTICACIÓN Y BASE
// ======================================================================

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

// ======================================================================
// DTOs DE PERFIL Y EDICIÓN (UserProfile, VendorModel)
// ======================================================================

// 1. DTO ANIDADO: Campos del objeto 'profile'
data class ProfileDetail(
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("control_number") val controlNumber: String?,
    val career: String?,
    @SerializedName("date_of_birth") val dateOfBirth: String?,
    @SerializedName("profile_image") val profileImage: String?,
    val role: String?
)

// 2. MODELO PRINCIPAL DE PERFIL (Respuesta del GET /api/users/profile/)
data class UserProfile(
    val id: Int,
    val username: String,
    val email: String,
    @SerializedName("first_name")
    val firstName: String,
    // Referencia a la estructura anidada
    val profile: ProfileDetail?
)

// 3. DTO DE PETICIÓN (PATCH) para Editar Perfil
data class UserProfileUpdate(
    @SerializedName("first_name") val firstName: String?,
    val email: String?,
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("date_of_birth") val dateOfBirth: String?,
    val password: String?
)

// 4. VENDOR MODEL (Usado en ProductModel como un objeto DTO simple)
data class VendorModel(
    @SerializedName("first_name")
    val firstName: String? = null,
    @SerializedName("profile_image")
    val profileImage: String? = null,
    val career: String? = null
)

// ======================================================================
// DTOs DE CARRITO Y RESEÑAS
// ======================================================================

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

data class CreateReviewRequest(
    val product: Int,
    val rating: Int,
    val comment: String
)

data class UpdateReviewRequest(
    val rating: Int,
    val comment: String,
    val id: Int,
    val product: Int
)