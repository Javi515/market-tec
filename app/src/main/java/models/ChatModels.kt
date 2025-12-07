package com.example.markettecnm.models

import com.google.gson.annotations.SerializedName

// Respuesta de start_chat y get_chat

// Datos del remitente dentro del mensaje
data class SenderData(
    val first_name: String?,
    val profile_image: String?,
    val career: String?
)

// Modelo completo del mensaje (GET)
