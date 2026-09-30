package com.example.equipo_dinamita.modelo

// MVC: MODELO
// Respuesta de la API cuando creo o edito un producto. Solo me interesa el id
// (la FakeStoreAPI en el POST regresa el nuevo id y no guarda nada de verdad).
// Le pongo valor por defecto para que Gson no truene si algún campo no viene.
data class ProductResponse(
    val id: Int = 0
)
