package com.example.equipo_dinamita.modelo

// MVC: MODELO
// US06 y US07: estos son los datos que mando a la API cuando creo (POST)
// o edito (PUT) un producto. No lleva "id" porque en el POST lo genera la API
// y en el PUT el id ya va en la URL (/products/{id}).
data class ProductRequest(
    val title: String,
    val price: Double,
    val description: String,
    val image: String,
    val category: String
)
