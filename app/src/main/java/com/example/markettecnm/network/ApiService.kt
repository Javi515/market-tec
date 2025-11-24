package com.example.markettecnm.network

// Importamos los modelos que están en la carpeta 'models' (ProductModel, ReviewModel, etc)
import com.example.markettecnm.models.*
// Importamos los modelos de autenticación que dejaste en 'network' (LoginRequestBody, etc)
import com.example.markettecnm.network.* import retrofit2.Response
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

    // CAMBIO: Usamos ProductModel ya que contiene toda la info necesaria
    @GET("api/products/{id}/")
    suspend fun getProductDetail(@Path("id") productId: Int): Response<ProductModel>

    @GET("api/categories/")
    suspend fun getCategories(): Response<List<CategoryModel>>

    // ========== FAVORITOS ==========
    // Nota: Asegúrate de tener las data class FavoriteResponse y AddFavoriteRequest
    // Si no las tienes aún, puedes comentar estas líneas temporalmente.
    @GET("api/favorites/")
    suspend fun getFavorites(): Response<List<FavoriteResponse>>

    @POST("api/favorites/")
    suspend fun addFavorite(@Body request: AddFavoriteRequest): Response<FavoriteResponse>

    @DELETE("api/favorites/{id}/")
    suspend fun removeFavorite(@Path("id") favoriteId: Int): Response<Unit>

    // ========== AUTENTICACIÓN ==========
    @POST("api/token/")
    suspend fun loginUser(@Body requestBody: LoginRequestBody): Response<TokenResponse>

    @POST("api/register/")
    suspend fun registerUser(@Body requestBody: RegistrationRequestBody): Response<RegistrationResponse>

    // ========== COMENTARIOS (REVIEWS) ==========
    // ESTO ES LO QUE NECESITAMOS PARA EL CARRUSEL DE TENDENCIAS 👇
    @GET("api/reviews/")
    suspend fun getReviews(): Response<List<ReviewModel>>

    // Nota: Si aún no tienes "CreateReviewRequest" creado, puedes comentar esto
    @POST("api/reviews/")
    suspend fun postReview(@Body review: CreateReviewRequest): Response<Void>

    // Nota: Si aún no tienes "UpdateReviewRequest" creado, puedes comentar esto
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
        .addInterceptor(AuthInterceptor()) // Asegúrate de tener tu clase AuthInterceptor
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