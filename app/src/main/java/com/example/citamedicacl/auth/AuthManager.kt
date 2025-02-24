package com.example.citamedicacl.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.citamedicacl.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.citamedicacl.App
import com.example.citamedicacl.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val patientsCollection = db.collection("patients")
    private val receptionistsCollection = db.collection("receptionists")
    private val prefs = App.prefs
    private lateinit var googleSignInClient: GoogleSignInClient

    init {
        // Verificar si hay una sesión guardada
        checkSavedSession()
    }

    private fun checkSavedSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // La sesión está activa
            val userType = prefs.getString("user_type", null)
            // Puedes usar esto para redirigir automáticamente
        }
    }

    suspend fun registerUser(email: String, password: String, name: String, userType: String): Result<User> {
        return try {
            // Validar el formato del email
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                return Result.failure(Exception("Formato de correo electrónico inválido"))
            }

            // Validar la longitud de la contraseña
            if (password.length < 6) {
                return Result.failure(Exception("La contraseña debe tener al menos 6 caracteres"))
            }

            try {
                // Crear usuario en Authentication
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid ?: throw Exception("Error al crear usuario")

                // Crear documento del usuario en la colección correspondiente
                val user = User(
                    id = userId,
                    name = name,
                    email = email,
                    role = userType
                )
                
                try {
                    // Seleccionar la colección según el tipo de usuario
                    val collection = when (userType) {
                        "patient" -> patientsCollection
                        "receptionist" -> receptionistsCollection
                        else -> throw Exception("Tipo de usuario no válido")
                    }
                    
                    collection.document(userId).set(user).await()
                } catch (e: Exception) {
                    // Si falla la creación en Firestore, eliminar el usuario de Authentication
                    auth.currentUser?.delete()
                    throw Exception("Error al guardar los datos del usuario: ${e.message}")
                }

                Result.success(user)
            } catch (e: FirebaseAuthWeakPasswordException) {
                Result.failure(Exception("La contraseña es demasiado débil"))
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Result.failure(Exception("El correo electrónico no es válido"))
            } catch (e: FirebaseAuthUserCollisionException) {
                Result.failure(Exception("Ya existe una cuenta con este correo electrónico"))
            } catch (e: Exception) {
                Result.failure(Exception("Error en el registro: ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Error al obtener usuario")
            
            // Intentar obtener el usuario de la colección de pacientes
            var userDoc = patientsCollection.document(firebaseUser.uid).get().await()
            
            // Si no está en pacientes, buscar en recepcionistas
            if (!userDoc.exists()) {
                userDoc = receptionistsCollection.document(firebaseUser.uid).get().await()
            }

            if (!userDoc.exists()) {
                throw Exception("Usuario no encontrado")
            }

            val user = User(
                id = firebaseUser.uid,
                name = userDoc.getString("name") ?: "",
                email = userDoc.getString("email") ?: "",
                role = userDoc.getString("role") ?: "",
                phone = userDoc.getString("phone") ?: ""
            )
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        val userType = getCurrentUserType() ?: return null
        
        return User(
            id = firebaseUser.uid,
            name = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: "",
            role = userType,
            phone = ""
        )
    }

    fun logout() {
        auth.signOut()
        // Cerrar sesión de Google
        if (::googleSignInClient.isInitialized) {
            googleSignInClient.signOut()
        }
        // Limpiar las preferencias
        prefs.edit().remove("user_type").apply()
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    suspend fun getCurrentUserType(): String? {
        return auth.currentUser?.let { user ->
            try {
                // Intentar obtener de pacientes primero
                var doc = patientsCollection.document(user.uid).get().await()
                
                // Si no está en pacientes, buscar en recepcionistas
                if (!doc.exists()) {
                    doc = receptionistsCollection.document(user.uid).get().await()
                }
                
                doc.getString("role")
            } catch (e: Exception) {
                null
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun configureGoogleSignIn(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun handleGoogleSignInResult(data: Intent?): Result<User> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: Exception) {
            Log.w("AuthManager", "Google sign in failed", e)
            Result.failure(e)
        }
    }

    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Error al obtener usuario")

            // Verificar si el usuario ya existe en la colección de pacientes
            var userDoc = patientsCollection.document(firebaseUser.uid).get().await()

            if (!userDoc.exists()) {
                // Si no existe, crear nuevo usuario como paciente
                val newUser = User(
                    id = firebaseUser.uid,
                    name = account.displayName ?: "",
                    email = account.email ?: "",
                    role = "patient",
                    phone = ""
                )

                // Guardar en Firestore
                patientsCollection.document(firebaseUser.uid)
                    .set(newUser)
                    .await()

                Result.success(newUser)
            } else {
                // Si existe, retornar usuario existente
                Result.success(User(
                    id = firebaseUser.uid,
                    name = userDoc.getString("name") ?: "",
                    email = userDoc.getString("email") ?: "",
                    role = userDoc.getString("role") ?: "patient",
                    phone = userDoc.getString("phone") ?: ""
                ))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        return googleSignInClient
    }
} 