package com.example.citamedicacl

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import kotlinx.coroutines.launch
import com.example.citamedicacl.auth.AuthManager
import com.example.citamedicacl.patient.PatientActivity
import com.example.citamedicacl.receptionist.ReceptionistActivity
import android.widget.ImageView

class MainActivity : AppCompatActivity() {
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var patientRadioButton: MaterialRadioButton
    private lateinit var receptionistRadioButton: MaterialRadioButton
    private lateinit var loginButton: MaterialButton
    
    private val authManager = AuthManager()
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Verificar si hay una sesión activa
        lifecycleScope.launch {
            if (authManager.isUserLoggedIn()) {
                val userType = authManager.getCurrentUserType()
                when (userType) {
                    "patient" -> startPatientActivity()
                    "receptionist" -> startReceptionistActivity()
                }
                return@launch
            }
        }
        
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        patientRadioButton = findViewById(R.id.patientRadioButton)
        receptionistRadioButton = findViewById(R.id.receptionistRadioButton)
        loginButton = findViewById(R.id.loginButton)

        // Configurar Google Sign In
        authManager.configureGoogleSignIn(this)

        // Configurar el botón de inicio de sesión
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            
            when {
                email.isEmpty() -> {
                    emailEditText.error = "El correo es requerido"
                    return@setOnClickListener
                }
                password.isEmpty() -> {
                    passwordEditText.error = "La contraseña es requerida"
                    return@setOnClickListener
                }
                !patientRadioButton.isChecked && !receptionistRadioButton.isChecked -> {
                    Toast.makeText(this, "Por favor seleccione un tipo de usuario", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val userType = if (patientRadioButton.isChecked) "patient" else "receptionist"
            loginUser(email, password, userType)
        }

        // Configurar el botón de Google Sign In
        findViewById<ImageView>(R.id.googleSignInButton).setOnClickListener {
            if (patientRadioButton.isChecked) {
                startGoogleSignIn()
            } else {
                Toast.makeText(
                    this,
                    "El inicio de sesión con Google solo está disponible para pacientes",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Agregar el manejo del clic en el enlace de registro
        findViewById<TextView>(R.id.registerLink).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String, userType: String) {
        loginButton.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val result = authManager.loginUser(email, password)
                result.fold(
                    onSuccess = { user ->
                        if (user.role == userType) {
                            // Inicio de sesión exitoso, redirigir según el tipo de usuario
                            when (userType) {
                                "patient" -> startPatientActivity()
                                "receptionist" -> startReceptionistActivity()
                            }
                        } else {
                            Toast.makeText(this@MainActivity, 
                                "Tipo de usuario incorrecto", 
                                Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { e ->
                        Toast.makeText(this@MainActivity, 
                            "Error: ${e.message}", 
                            Toast.LENGTH_SHORT).show()
                    }
                )
            } finally {
                loginButton.isEnabled = true
            }
        }
    }

    private fun startPatientActivity() {
        val intent = Intent(this, PatientActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun startReceptionistActivity() {
        val intent = Intent(this, ReceptionistActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun startGoogleSignIn() {
        val signInIntent = authManager.getGoogleSignInIntent()
        // Forzar la selección de cuenta cada vez
        authManager.getGoogleSignInClient().signOut().addOnCompleteListener {
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            lifecycleScope.launch {
                try {
                    val result = authManager.handleGoogleSignInResult(data)
                    result.fold(
                        onSuccess = { user ->
                            startPatientActivity()
                        },
                        onFailure = { e ->
                            Toast.makeText(
                                this@MainActivity,
                                "Error al iniciar sesión con Google: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Salir")
            .setMessage("¿Estás seguro que deseas salir de la aplicación?")
            .setPositiveButton("Sí") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("No", null)
            .setIcon(R.drawable.ic_warning)
            .show()
    }
}