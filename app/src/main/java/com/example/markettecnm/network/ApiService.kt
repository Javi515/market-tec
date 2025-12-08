package com.example.markettecnm.network

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

// 👇 IMPORTS DE TUS MODELOS EXISTENTES
import com.example.markettecnm.models.ProductModel
import com.example.markettecnm.models.CategoryModel
import com.example.markettecnm.models.ReviewModel
import com.example.markettecnm.models.UserProfile
import com.example.markettecnm.models.UserProfileUpdate
import com.example.markettecnm.models.ChatResponse
import com.example.markettecnm.models.MessageRequest
import com.example.markettecnm.models.MessageResponse
import com.example.markettecnm.models.OrderResponse
import com.example.markettecnm.models.CreateOrderRequest

const val BASE_URL = "http://172.200.235.24/"

interface ApiService {

    // ========== 1. PRODUCTOS ==========

    @GET("api/products/")
    suspend fun getProducts(): Response<List<ProductModel>>

    @GET("api/products/{id}/")
    suspend fun getProductDetail(@Path("id") productId: Int): Response<ProductModel>

    @GET("api/products/")
    suspend fun searchProducts(@Query("q") query: String): Response<List<ProductModel>>

    @GET("api/categories/")
    suspend fun getCategories(): Response<List<CategoryModel>>

    @Multipart
    @POST("api/products/")
    suspend fun createProduct(
        @PartMap params: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?
    ): Response<ProductModel>

    @Multipart
    @PATCH("api/products/{id}/")
    suspend fun updateProduct(
        @Path("id") productId: Int,
        @PartMap params: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?
    ): Response<ProductModel>

    // Endpoint para marcar como agotado
    @PATCH("api/products/{id}/mark_out_of_stock/")
    suspend fun markProductAsSoldOut(
        @Path("id") productId: Int,
        @Query("q") productName: String, // 🟢 ESTE ERA EL PARÁMETRO FALTANTE
        @Body productBody: ProductModel
    ): Response<ProductModel>

    @DELETE("api/products/{id}/")
    suspend fun deleteProduct(@Path("id") productId: Int): Response<Unit>

    // ================== 2. PERFIL ==================

    @GET("api/users/profile/")
    suspend fun getMyProfile(): Response<UserProfile>

    @PATCH("api/users/profile/")
    suspend fun updateProfile(@Body request: UserProfileUpdate): Response<UserProfile>

    @Multipart
    @PATCH("api/users/profile/")
    suspend fun updateProfileImage(@Part image: MultipartBody.Part): Response<UserProfile>

    @GET("api/users/{id}/")
    suspend fun getUserById(@Path("id") userId: Int): Response<UserProfile>

    // ================== 3. FAVORITOS Y AUTH (TIPOS CORREGIDOS) ==================

    @GET("api/favorites/")
    suspend fun getFavorites(): Response<List<FavoriteResponse>> // 🟢 Ahora devuelve FavoriteResponse con .product

    @POST("api/favorites/toggle/")
    suspend fun toggleFavorite(@Body request: ToggleFavoriteRequest): Response<Void>

    @POST("api/token/")
    suspend fun loginUser(@Body requestBody: LoginRequestBody): Response<TokenResponse>

    @POST("api/register/")
    suspend fun registerUser(@Body requestBody: RegistrationRequestBody): Response<RegistrationResponse>

    // ================== 4. RESEÑAS ==================

    @GET("api/reviews/")
    suspend fun getReviews(): Response<List<ReviewModel>>

    @POST("api/reviews/")
    suspend fun postReview(@Body review: CreateReviewRequest): Response<Void>

    @PATCH("api/reviews/{id}/")
    suspend fun updateReview(
        @Path("id") reviewId: Int,
        @Body request: UpdateReviewRequest
    ): Response<ReviewModel>

    @DELETE("api/reviews/{id}/")
    suspend fun deleteReview(@Path("id") reviewId: Int): Response<Unit>

    // ================== 5. SISTEMA DE CHAT ==================

    @POST("api/chat/start_chat/")
    suspend fun startChat(
        @Query("target_user_id") targetUserId: Int,
        @Body dummyBody: Map<String, String>
    ): Response<ChatResponse>

    @GET("api/messages/")
    suspend fun getMessages(@Query("conversation") conversationId: Int): Response<List<MessageResponse>>

    @POST("api/messages/")
    suspend fun sendMessage(@Body request: MessageRequest): Response<MessageResponse>

    // ================== 6. VENTAS Y ÓRDENES ==================

    @POST("api/orders/")
    suspend fun createOrder(@Body order: CreateOrderRequest): Response<OrderResponse>

    @GET("api/orders/my-sales/")
    suspend fun getMySales(): Response<List<OrderResponse>>

    @GET("api/orders/")
    suspend fun getOrders(): Response<List<OrderResponse>>

    @POST("api/orders/{id}/cancel_order/")
    suspend fun cancelOrder(
        @Path("id") orderId: Int,
        @Body dummyBody: Map<String, String>
    ): Response<OrderResponse>
}

object RetrofitClient {
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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

