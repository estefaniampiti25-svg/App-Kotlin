package com.example.equipo_dinamita.modelo

// MVC: MODELO
// Usuario que viene de GET /users. Lo uso para sacar el id real del usuario
// y con eso decidir su rol (US01).
data class User(
    val id: Int,
    val email: String,
    val username: String
)
