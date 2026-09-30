package com.example.equipo_dinamita.modelo

// MVC: MODELO
// Datos que se mandan a POST /auth/login (US01)
data class LoginRequest(
    val username: String, // usuario que escribió la persona
    val password: String  // contraseña que escribió la persona
)
