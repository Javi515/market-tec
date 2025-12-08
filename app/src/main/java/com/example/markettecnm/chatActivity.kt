package com.example.markettecnm

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ChatAdapter
import com.example.markettecnm.models.MessageModel
import com.example.markettecnm.models.MessageRequest
import com.example.markettecnm.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatActivity : AppCompatActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var etMessageInput: EditText
    private lateinit var btnSend: Button
    private lateinit var chatAdapter: ChatAdapter

    private var conversationId: Int = -1
    private var myUserId: Int = -1
    private val messageList = mutableListOf<MessageModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // 1. Obtener datos del Intent
        conversationId = intent.getIntExtra("conversation_id", -1)
        val chatTitle = intent.getStringExtra("chat_title") ?: "Chat"

        // 2. Recuperar Mi ID (Login ID)
        val prefs = getSharedPreferences("markettec_prefs", Context.MODE_PRIVATE)
        myUserId = prefs.getInt("current_user_id", -1)

        Log.d("CHAT_ACT", "Entrando a chat: $conversationId | Mi User ID: $myUserId")

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbarChat)
        toolbar.title = chatTitle
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Vistas
        rvChat = findViewById(R.id.rvChat)
        etMessageInput = findViewById(R.id.etMessageInput)
        btnSend = findViewById(R.id.btnSend)

        // Adapter
        chatAdapter = ChatAdapter(messageList)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvChat.layoutManager = layoutManager
        rvChat.adapter = chatAdapter

        // Cargar
        if (conversationId != -1) {
            loadMessages()
        } else {
            Toast.makeText(this, "Error: Conversación no válida", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnSend.setOnClickListener {
            val text = etMessageInput.text.toString().trim()
            if (text.isNotEmpty() && conversationId != -1) {
                sendMessage(text)
            }
        }
    }

    private fun loadMessages() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getMessages(conversationId)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        messageList.clear()
                        val apiMessages = response.body()!!

                        apiMessages.forEach { msg ->

                            // 🟢 CORRECCIÓN: Usamos 'userId' para coincidir con models.kt
                            val isMine = if (msg.senderData != null) {
                                (msg.senderData.userId == myUserId) // <--- CAMBIO AQUÍ
                            } else {
                                (msg.sender == myUserId)
                            }

                            // 🟢 FIX VISUAL: Mostrar texto placeholder si es imagen
                            val displayContent = if (!msg.text.isNullOrEmpty()) {
                                msg.text
                            } else if (msg.image != null) {
                                "📷 Imagen enviada"
                            } else {
                                "..."
                            }

                            messageList.add(MessageModel(
                                content = displayContent,
                                isMine = isMine
                            ))
                        }
                        chatAdapter.notifyDataSetChanged()
                        if (messageList.isNotEmpty()) {
                            rvChat.scrollToPosition(messageList.size - 1)
                        }
                    } else {
                        val error = response.errorBody()?.string()
                        Log.e("CHAT_ACT", "Error API: ${response.code()} $error")
                        Toast.makeText(this@ChatActivity, "Error al cargar mensajes", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CHAT_ACT", "Excepción de red", e)
                    Toast.makeText(this@ChatActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendMessage(text: String) {
        etMessageInput.text.clear()

        val tempMsg = MessageModel(text, true)
        chatAdapter.addMessage(tempMsg)
        rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = MessageRequest(conversationId, text)
                val response = RetrofitClient.instance.sendMessage(request)

                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ChatActivity, "Error al enviar", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CHAT_ACT", "Error de red", e)
                }
            }
        }
    }
}