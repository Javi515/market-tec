package com.example.markettecnm.adapters

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.R
import com.example.markettecnm.models.MessageModel

class ChatAdapter(private val messages: MutableList<MessageModel>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.messageContainer)
        val tvContent: TextView = view.findViewById(R.id.tvMessageContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.tvContent.text = message.content

        if (message.isMine) {
            // Mensaje Mío: Alinear a la DERECHA y color AZUL
            holder.container.gravity = Gravity.END
            holder.tvContent.setBackgroundColor(Color.parseColor("#2196F3")) // Azul
        } else {
            // Mensaje del Vendedor: Alinear a la IZQUIERDA y color GRIS
            holder.container.gravity = Gravity.START
            holder.tvContent.setBackgroundColor(Color.parseColor("#757575")) // Gris
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: MessageModel) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
}