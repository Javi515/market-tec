package com.example.markettecnm.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // 👇 Usa tu IP local (la del paso anterior)
    private const val BASE_URL = "http://192.168.1.65:8000/api/"

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

