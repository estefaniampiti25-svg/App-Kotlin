package com.example.equipo_dinamita.config

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Configuración de Retrofit (acceso a datos, apoya al controlador del MVC).
// Uso un object para tener una sola instancia en toda la app.
object RetrofitClient {
    // URL base de la API, todas las rutas de ApiService se le suman a esta
    private const val BASE_URL = "https://fakestoreapi.com/"

    // "by lazy" hace que Retrofit se cree hasta que se usa por primera vez
    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // Gson convierte el JSON de la API a mis clases del modelo (y al revés)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
