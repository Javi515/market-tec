package com.example.markettecnm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.adapters.ChatListAdapter
import com.example.markettecnm.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatListActivity : AppCompatActivity() {

    private lateinit var rvChatList: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: ChatListAdapter
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        val prefs = getSharedPreferences("markettec_prefs", Context.MODE_PRIVATE)
        currentUserId = prefs.getInt("current_user_id", -1)

        setupToolbar()

        rvChatList = findViewById(R.id.rvChatList)
        tvEmpty = findViewById(R.id.tvEmpty)

        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadChats()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar?.apply {
            title = "Mis Mensajes"
            navigationIcon = ContextCompat.getDrawable(
                this@ChatListActivity,
                androidx.appcompat.R.drawable.abc_ic_ab_back_material
            )
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
    }

    private fun setupRecyclerView() {
        rvChatList.layoutManager = LinearLayoutManager(this)

        adapter = ChatListAdapter(emptyList()) { chat ->
            // Al hacer clic, abrimos el chat individual
            val otherUser = chat.otherUser
            val chatTitle = otherUser.firstName ?: otherUser.username ?: "Chat"

            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("conversation_id", chat.id)
                putExtra("chat_title", chatTitle)
            }
            startActivity(intent)
        }
        rvChatList.adapter = adapter
    }

    private fun loadChats() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Llamada al endpoint: GET api/chat/
                val response = RetrofitClient.instance.getMyChats()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val chats = response.body()!!

                        if (chats.isNotEmpty()) {
                            tvEmpty.visibility = View.GONE
                            rvChatList.visibility = View.VISIBLE
                            adapter.updateData(chats)
                        } else {
                            rvChatList.visibility = View.GONE
                            tvEmpty.visibility = View.VISIBLE
                            tvEmpty.text = "No tienes conversaciones activas."
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("CHAT_LIST", "Error API: ${response.code()} | $errorBody")
                        Toast.makeText(this@ChatListActivity, "Error al cargar chats: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CHAT_LIST", "Excepción de red", e)
                    // Este Toast te dirá si es un error de Parsing o de Conexión
                    Toast.makeText(this@ChatListActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}