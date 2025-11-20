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
import de.hdodenhof.circleimageview.CircleImageView

class ReviewAdapter(
    private val reviews: List<ReviewModel>,
    private val currentUserName: String, // <-- usuario actual
    private val onActionClick: (String, ReviewModel) -> Unit // <-- callback para edit/delete
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivReviewerImage: CircleImageView = view.findViewById(R.id.ivReviewerImage)
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
        val reviewer = review.reviewer

        holder.tvReviewerName.text = reviewer.firstName
        holder.tvReviewerCareer.text = reviewer.career ?: ""
        holder.tvComment.text = review.comment
        holder.ratingBar.rating = review.rating.toFloat()

        Glide.with(holder.itemView.context)
            .load(reviewer.profileImage)
            .placeholder(R.drawable.ic_placeholder_profile)
            .error(R.drawable.ic_placeholder_profile)
            .circleCrop()
            .into(holder.ivReviewerImage)

        // Mostrar menú solo si el comentario es del usuario actual
        if (reviewer.firstName == currentUserName) {
            holder.btnOptions.visibility = View.VISIBLE
            holder.btnOptions.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menuInflater.inflate(R.menu.menu_review_options, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_edit -> {
                            onActionClick("edit", review) // notifica a la Activity
                            true
                        }
                        R.id.action_delete -> {
                            onActionClick("delete", review) // notifica a la Activity
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