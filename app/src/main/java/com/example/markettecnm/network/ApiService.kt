package com.example.markettecnm.network

import com.example.markettecnm.models.*
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.OkHttpClient

const val BASE_URL = "http://172.200.235.24/"

interface ApiService {

    @GET("api/products/")
    suspend fun getProducts(): Response<List<ProductModel>>

    @GET("api/products/{id}/")
    suspend fun getProductDetail(@Path("id") productId: Int): Response<ProductModel>

    @GET("api/categories/")
    suspend fun getCategories(): Response<List<CategoryModel>>

    // CREAR PRODUCTO
    @Multipart
    @POST("api/products/")
    suspend fun createProduct(
        @PartMap params: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?
    ): Response<ProductModel>

    // ACTUALIZAR PRODUCTO
    @Multipart
    @PUT("api/products/{id}/")
    suspend fun updateProduct(
        @Path("id") productId: Int,
        @PartMap params: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?
    ): Response<ProductModel>

    // ELIMINAR PRODUCTO (NUEVO)
    @DELETE("api/products/{id}/")
    suspend fun deleteProduct(@Path("id") productId: Int): Response<Unit> // <-- AGREGADO

    // ================== PERFIL Y AUTENTICACIÓN ==================
    @GET("api/users/profile/")
    suspend fun getMyProfile(): Response<UserProfile>

    @GET("api/favorites/")
    suspend fun getFavorites(): Response<List<FavoriteResponse>>

    @POST("api/favorites/toggle/")
    suspend fun toggleFavorite(@Body request: ToggleFavoriteRequest): Response<Void>

    @POST("api/token/")
    suspend fun loginUser(@Body requestBody: LoginRequestBody): Response<TokenResponse>

    @POST("api/register/")
    suspend fun registerUser(@Body requestBody: RegistrationRequestBody): Response<RegistrationResponse>

    // ================== RESEÑAS ==================
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

object RetrofitClient {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
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