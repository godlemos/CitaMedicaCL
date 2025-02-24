package com.example.citamedicacl

import android.os.Bundle
import android.widget.Toast
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.radiobutton.MaterialRadioButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.example.citamedicacl.auth.AuthManager
import android.app.ProgressDialog
import kotlinx.coroutines.withContext
import android.content.Intent

class RegisterActivity : AppCompatActivity() {
    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var registerButton: MaterialButton
    private lateinit var patientRadioButton: MaterialRadioButton
    private lateinit var receptionistRadioButton: MaterialRadioButton
    private val authManager = AuthManager()
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inicializar vistas
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailRegisterEditText)
        passwordEditText = findViewById(R.id.passwordRegisterEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        registerButton = findViewById(R.id.registerButton)
        patientRadioButton = findViewById(R.id.patientRegisterRadioButton)
        receptionistRadioButton = findViewById(R.id.receptionistRegisterRadioButton)

        // Agregar el manejo del botón de retroceso
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            when {
                name.isEmpty() -> {
                    nameEditText.error = "El nombre es requerido"
                    return@setOnClickListener
                }
                email.isEmpty() -> {
                    emailEditText.error = "El correo es requerido"
                    return@setOnClickListener
                }
                password.isEmpty() -> {
                    passwordEditText.error = "La contraseña es requerida"
                    return@setOnClickListener
                }
                confirmPassword.isEmpty() -> {
                    confirmPasswordEditText.error = "Debe confirmar la contraseña"
                    return@setOnClickListener
                }
                password != confirmPassword -> {
                    confirmPasswordEditText.error = "Las contraseñas no coinciden"
                    return@setOnClickListener
                }
                !patientRadioButton.isChecked && !receptionistRadioButton.isChecked -> {
                    Toast.makeText(this, "Por favor seleccione un tipo de usuario", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val userType = if (patientRadioButton.isChecked) "patient" else "receptionist"
            registerUser(name, email, password, userType)
        }
    }

    private fun registerUser(name: String, email: String, password: String, userType: String) {
        registerButton.isEnabled = false
        
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Registrando usuario...")
            setCancelable(false)
            show()
        }
        
        scope.launch {
            try {
                val result = authManager.registerUser(email, password, name, userType)
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    result.fold(
                        onSuccess = { 
                            Toast.makeText(
                                this@RegisterActivity, 
                                "Registro exitoso", 
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            // Volver al login principal
                            val intent = Intent(this@RegisterActivity, MainActivity::class.java).apply {
                                // Limpiar el stack de actividades para que no pueda volver atrás
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        },
                        onFailure = { e ->
                            val errorMessage = when {
                                e.message?.contains("contraseña") == true -> e.message
                                e.message?.contains("correo") == true -> e.message
                                else -> "Error en el registro: ${e.message}"
                            }
                            Toast.makeText(
                                this@RegisterActivity,
                                errorMessage,
                                Toast.LENGTH_LONG
                            ).show()
                            registerButton.isEnabled = true
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@RegisterActivity,
                        "Error inesperado: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    registerButton.isEnabled = true
                }
            }
        }
    }
} 