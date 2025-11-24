package com.example.markettecnm

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class FullScreenImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        // Recibimos la URL que nos manda la otra pantalla
        val imageUrl = intent.getStringExtra("image_url")

        val ivFullScreen = findViewById<ImageView>(R.id.ivFullScreen)
        val btnClose = findViewById<ImageButton>(R.id.btnClose)

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .into(ivFullScreen)
        }

        btnClose.setOnClickListener {
            finish() // Cierra esta pantalla y regresa al producto
        }
    }
}