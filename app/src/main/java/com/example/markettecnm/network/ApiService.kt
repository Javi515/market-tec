package com.example.markettecnm.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// --- Auth ---
data class LoginRequest(
    val username: String,
    val password: String
)

data class TokenResponse(
    val access: String,
    val refresh: String
)

// --- Users ---
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String?
    // Si el backend manda más campos, Gson los ignora sin problema
)

interface ApiService {
    @POST("token/")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @GET("users/")
    suspend fun getUsers(
        @Header("Authorization") bearerToken: String
    ): List<UserResponse>
}
