package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.markettecnm.R
import com.example.markettecnm.models.ReviewModel

class ReviewAdapter(
    private val reviews: List<ReviewModel>,
    private val currentUserName: String,
    private val onActionClick: (String, ReviewModel) -> Unit
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Usamos ImageView estándar (Glide se encarga del círculo)
        val ivReviewerImage: ImageView = view.findViewById(R.id.ivReviewerImage)
        val tvReviewerName: TextView = view.findViewById(R.id.tvReviewerName)
        val tvReviewerCareer: TextView = view.findViewById(R.id.tvReviewerCareer)
        val tvComment: TextView = view.findViewById(R.id.tvComment)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        val btnOptions: ImageView = view.findViewById(R.id.btnOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]

        // 1. CORRECCIÓN: Acceso seguro al reviewer (puede ser null)
        val reviewer = review.reviewer

        // Usamos el operador Elvis (?:) para valores por defecto
        val name = reviewer?.firstName ?: "Anónimo"
        val career = reviewer?.career ?: ""
        val image = reviewer?.profileImage

        holder.tvReviewerName.text = name
        holder.tvReviewerCareer.text = career
        holder.tvComment.text = review.comment
        holder.ratingBar.rating = review.rating.toFloat()

        // 2. Carga de imagen segura con Glide
        if (!image.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(image)
                .circleCrop() // Esto hace la imagen redonda sin necesitar librerías extra
                .placeholder(android.R.drawable.sym_def_app_icon) // Icono por defecto sistema
                .error(android.R.drawable.sym_def_app_icon)
                .into(holder.ivReviewerImage)
        } else {
            holder.ivReviewerImage.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        // 3. Lógica del Menú (Solo si el nombre coincide)
        // Comparamos con seguridad usando '?'
        if (reviewer?.firstName == currentUserName) {
            holder.btnOptions.visibility = View.VISIBLE
            holder.btnOptions.setOnClickListener { view ->
                // Asegúrate de tener res/menu/menu_review_options.xml creado
                val popup = PopupMenu(view.context, view)
                popup.menuInflater.inflate(R.menu.menu_review_options, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_edit -> {
                            onActionClick("edit", review)
                            true
                        }
                        R.id.action_delete -> {
                            onActionClick("delete", review)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        } else {
            holder.btnOptions.visibility = View.GONE
        }
    }

    override fun getItemCount() = reviews.size
}