package com.example.citamedicacl

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.citamedicacl.auth.AuthManager
import com.example.citamedicacl.patient.PatientActivity
import com.example.citamedicacl.receptionist.ReceptionistActivity
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Animar los elementos
        val logoImageView = findViewById<ImageView>(R.id.logoImageView)
        val titleTextView = findViewById<TextView>(R.id.titleTextView)
        val subtitleTextView = findViewById<TextView>(R.id.subtitleTextView)

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        logoImageView.startAnimation(fadeIn)
        titleTextView.startAnimation(slideUp)
        subtitleTextView.startAnimation(slideUp)

        // Navegar a la siguiente pantalla después de un delay
        Handler(Looper.getMainLooper()).postDelayed({
            lifecycleScope.launch {
                checkUserAndNavigate()
            }
        }, 2000)
    }

    private suspend fun checkUserAndNavigate() {
        val currentUser = authManager.getCurrentUser()
        val intent = if (currentUser != null) {
            when (currentUser.role) {
                "patient" -> Intent(this, PatientActivity::class.java)
                "receptionist" -> Intent(this, ReceptionistActivity::class.java)
                else -> Intent(this, MainActivity::class.java)
            }
        } else {
            Intent(this, MainActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
} 