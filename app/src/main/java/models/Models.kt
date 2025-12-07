package com.example.markettecnm.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// ==========================================
// 1. PRODUCTOS Y CATEGORÍAS
// ==========================================

data class ProductModel(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("price") val price: String,
    @SerializedName("inventory") val inventory: String,
    @SerializedName("user") val user: Int,
    @SerializedName("status") val status: String,

    @SerializedName("vendor") val vendor: VendorModel,

    // 👇 ESTE ES EL CAMPO NUEVO NECESARIO PARA FILTRAR
    @SerializedName("category") val category: Int,

    @SerializedName("category_name") val categoryName: String,
    @SerializedName("product_image") val image: String? = null
)

data class CategoryModel(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("image") val image: String? = null
)

// Definición de VendorModel (Necesaria aquí para que ProductModel funcione)
data class VendorModel(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("profile_image") val profileImage: String? = null,
    val career: String? = null
) : Serializable

// ==========================================
// 2. RESEÑAS
// ==========================================

data class ReviewModel(
    val id: Int,
    @SerializedName("product") val product: Int,
    val rating: Int,
    val comment: String?,
    @SerializedName("created_at") val createdAt: String? = null,
    val reviewer: Reviewer? = null
)

data class Reviewer(
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("profile_image") val profileImage: String?,
    val career: String?
)

// ==========================================
// 3. PERFIL DE USUARIO Y BANEO (LECTURA)
// ==========================================

data class UserProfile(
    val id: Int,
    val username: String,
    val email: String,
    @SerializedName("first_name") val firstName: String?,
    val profile: ProfileDetail?
) : Serializable

data class ProfileDetail(
    val role: String?,
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("control_number") val controlNumber: String?,
    val career: String?,
    @SerializedName("date_of_birth") val dateOfBirth: String?,
    @SerializedName("profile_image") val profileImage: String?,

    // Campos de Baneo
    @SerializedName("is_banned") val isBanned: Boolean? = false,
    @SerializedName("ban_reason") val banReason: String? = null
) : Serializable

// ==========================================
// 4. ACTUALIZACIÓN DE PERFIL (ESCRITURA)
// ==========================================

data class UserProfileUpdate(
    @SerializedName("first_name")
    val firstName: String?,

    val email: String? = null,
    val password: String? = null,

    // 👇 Enviamos los datos extra dentro de un objeto 'profile'
    @SerializedName("profile")
    val profile: ProfileUpdateData
)

// Clase auxiliar para agrupar los datos del perfil
data class ProfileUpdateData(
    @SerializedName("phone_number")
    val phoneNumber: String?,

    @SerializedName("date_of_birth")
    val dateOfBirth: String?,

    @SerializedName("career")
    val career: String?
)

// ==========================================
// 5. CHAT (Para que ApiService no falle)
// ==========================================

data class ChatResponse(
    val id: Int,
    val user_a: Int,
    val user_b: Int,
    val other_user: String?,
    val last_message: String?,
    val updated_at: String?
)

data class MessageResponse(
    val id: Int,
    val conversation: Int,
    val sender: Int,
    val text: String?,
    val image: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("is_read") val isRead: Boolean
)

data class MessageRequest(
    val conversation: Int,
    val text: String
)