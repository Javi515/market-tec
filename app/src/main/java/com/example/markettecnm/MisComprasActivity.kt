package com.example.markettecnm

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.appbar.MaterialToolbar

class MisComprasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mis_compras)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.navigationIcon = AppCompatResources.getDrawable(
            this, androidx.appcompat.R.drawable.abc_ic_ab_back_material
        )
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
}
