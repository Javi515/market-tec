package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.markettecnm.R
import com.example.markettecnm.models.ChatResponse

class ChatListAdapter(
    private var chats: List<ChatResponse>,
    private val onChatClick: (ChatResponse) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    fun updateData(newChats: List<ChatResponse>) {
        chats = newChats
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_conversation, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]

        // 1. Obtener datos del otro usuario
        val otherUser = chat.otherUser
        val displayName = otherUser.firstName?.takeIf { it.isNotEmpty() }
            ?: otherUser.username
            ?: "Usuario Desconocido"

        holder.tvName.text = displayName

        // 2. Obtener último mensaje
        val lastMsgText = chat.lastMessage?.text
        if (!lastMsgText.isNullOrEmpty()) {
            holder.tvLastMessage.text = lastMsgText
        } else if (chat.lastMessage?.image != null) {
            holder.tvLastMessage.text = "📷 Imagen enviada"
        } else {
            holder.tvLastMessage.text = "Iniciar conversación..."
        }

        // 3. Fecha (updated_at viene del modelo nuevo)
        holder.tvDate.text = chat.updated_at?.take(10) ?: ""

        // 4. Imagen de perfil (Corrección de URL)
        val profileImg = otherUser.profileImage
        if (!profileImg.isNullOrEmpty()) {
            // Aseguramos que la URL esté completa
            // CAMBIA ESTA IP SI ES DIFERENTE EN TU CASA
            val baseUrl = "http://172.200.235.24"

            val fullUrl = when {
                profileImg.startsWith("http") -> profileImg
                profileImg.startsWith("/") -> "$baseUrl$profileImg"
                else -> "$baseUrl/$profileImg"
            }

            Glide.with(holder.itemView.context)
                .load(fullUrl)
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .error(android.R.drawable.ic_menu_myplaces)
                .into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces)
        }

        // 5. Clic en el item
        holder.itemView.setOnClickListener {
            onChatClick(chat)
        }
    }

    override fun getItemCount() = chats.size

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivChatUserAvatar)
        val tvName: TextView = view.findViewById(R.id.tvChatUserName)
        val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage)
        val tvDate: TextView = view.findViewById(R.id.tvChatDate)
    }
}