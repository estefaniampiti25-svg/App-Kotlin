package com.example.equipo_dinamita.modelo

// MVC: MODELO
// Lo que responde /auth/login cuando las credenciales son correctas: solo el token
data class LoginResponse(
    val token: String
)
