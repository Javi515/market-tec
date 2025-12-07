package com.example.markettecnm.models

data class MessageModel(
    val content: String,
    val isMine: Boolean, // true = enviado por mi (derecha), false = recibido (izquierda)
    val timestamp: Long = System.currentTimeMillis()
)