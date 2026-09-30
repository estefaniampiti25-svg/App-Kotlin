package com.example.equipo_dinamita.modelo

// MVC: MODELO
// Esta clase guarda los datos de un producto tal como los regresa la FakeStoreAPI
// (GET /products y GET /products/{id}). Solo son datos, no tiene lógica.
data class Product(
    val id: Int,             // id que le asigna la API
    val title: String,       // nombre del producto
    val price: Double,       // precio (en dólares)
    val description: String, // descripción larga
    val category: String,    // categoría (electronics, jewelery, etc.)
    val image: String        // URL de la imagen
)
