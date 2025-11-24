package com.example.markettecnm.network

import com.example.markettecnm.models.*
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

const val BASE_URL = "http://172.200.235.24/"

interface ApiService {

    // ========== PRODUCTOS Y CATEGORÍAS ==========
    @GET("api/products/")
    suspend fun getProducts(): Response<List<ProductModel>>

    @GET("api/products/{id}/")
    suspend fun getProductDetail(@Path("id") productId: Int): Response<ProductModel>

    @GET("api/categories/")
    suspend fun getCategories(): Response<List<CategoryModel>>

    // ========== FAVORITOS (ACTUALIZADO) ==========

    @GET("api/favorites/")
    suspend fun getFavorites(): Response<List<FavoriteResponse>>

    // 👇 NUEVO ENDPOINT: Toggle (Alternar Favorito)
    // Usa la clase ToggleFavoriteRequest que creamos en DataModels.kt
    @POST("api/favorites/toggle/")
    suspend fun toggleFavorite(@Body request: ToggleFavoriteRequest): Response<Void>

    /* MÉTODOS ANTIGUOS (Los dejo comentados por si acaso)
    @POST("api/favorites/")
    suspend fun addFavorite(@Body request: AddFavoriteRequest): Response<FavoriteResponse>

    @DELETE("api/favorites/{id}/")
    suspend fun removeFavorite(@Path("id") favoriteId: Int): Response<Unit>
    */

    // ========== AUTENTICACIÓN ==========
    @POST("api/token/")
    suspend fun loginUser(@Body requestBody: LoginRequestBody): Response<TokenResponse>

    @POST("api/register/")
    suspend fun registerUser(@Body requestBody: RegistrationRequestBody): Response<RegistrationResponse>

    // ========== COMENTARIOS (REVIEWS) ==========
    @GET("api/reviews/")
    suspend fun getReviews(): Response<List<ReviewModel>>

    @POST("api/reviews/")
    suspend fun postReview(@Body review: CreateReviewRequest): Response<Void>

    @PUT("api/reviews/{id}/")
    suspend fun updateReview(
        @Path("id") reviewId: Int,
        @Body request: UpdateReviewRequest
    ): Response<ReviewModel>

    @DELETE("api/reviews/{id}/")
    suspend fun deleteReview(@Path("id") reviewId: Int): Response<Unit>
}

/**
 * Singleton para el cliente Retrofit
 */
object RetrofitClient {

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor()) // Tu interceptor para el token
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}