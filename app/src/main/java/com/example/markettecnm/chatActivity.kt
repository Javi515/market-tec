package com.example.markettecnm

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ChatAdapter
import com.example.markettecnm.models.MessageModel // Tu modelo local para el adapter
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
    private var myUserId: Int = -1 // Necesitamos saber quién soy yo
    private val messageList = mutableListOf<MessageModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // 1. Obtener datos del Intent y Preferencias
        conversationId = intent.getIntExtra("conversation_id", -1)
        val chatTitle = intent.getStringExtra("chat_title") ?: "Chat"

        // RECUPERAR MI ID (Ajusta esto según como guardes tu sesión)
        val prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        myUserId = prefs.getInt("id", -1)

        // Configurar Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbarChat)
        toolbar.title = chatTitle
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // Vistas
        rvChat = findViewById(R.id.rvChat)
        etMessageInput = findViewById(R.id.etMessageInput)
        btnSend = findViewById(R.id.btnSend)

        // Configurar RecyclerView
        chatAdapter = ChatAdapter(messageList)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true // Mensajes pegados abajo
        rvChat.layoutManager = layoutManager
        rvChat.adapter = chatAdapter

        // Lógica
        if (conversationId != -1) {
            loadMessages() // Cargar historial
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
                // GET api/messages/?conversation={id}
                val response = RetrofitClient.instance.getMessages(conversationId)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        messageList.clear()
                        val apiMessages = response.body()!!

                        // Convertir respuesta de API a Modelo Visual del Adapter
                        apiMessages.forEach { msg ->
                            val isMine = (msg.sender == myUserId)
                            // Creamos el objeto visual
                            messageList.add(MessageModel(
                                content = msg.text ?: "",
                                isMine = isMine
                            ))
                        }
                        chatAdapter.notifyDataSetChanged()
                        rvChat.scrollToPosition(messageList.size - 1)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendMessage(text: String) {
        etMessageInput.text.clear() // Limpiar input visualmente rápido

        // Agregar visualmente primero (Optimistic UI)
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
                        // Aquí podrías marcar el mensaje con error visualmente
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}