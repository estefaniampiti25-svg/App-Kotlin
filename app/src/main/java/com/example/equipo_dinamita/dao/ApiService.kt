package com.example.equipo_dinamita.dao

import com.example.equipo_dinamita.modelo.LoginRequest
import com.example.equipo_dinamita.modelo.LoginResponse
import com.example.equipo_dinamita.modelo.Product
import com.example.equipo_dinamita.modelo.ProductRequest
import com.example.equipo_dinamita.modelo.ProductResponse
import com.example.equipo_dinamita.modelo.User
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// DAO: aquí declaro todos los endpoints de la FakeStoreAPI.
// Los usa el controlador (y por ahora también las Activities de login/catálogo).
interface ApiService {
    // US01 - login
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // US01 - lista de usuarios para sacar el id y mapear el rol
    @GET("users")
    fun getUsers(): Call<List<User>>

    // US03 - catálogo general
    @GET("products")
    fun getAllProducts(): Call<List<Product>>

    // US04 - Filtrar productos por categoría
    @GET("products/categories")
    fun getCategories(): Call<List<String>>

    @GET("products/category/{category}")
    fun getProductsByCategory(@Path("category") category: String): Call<List<Product>>

    // US05 - Ver detalle del producto
    @GET("products/{id}")
    fun getProductById(@Path("id") id: Int): Call<Product>

    // US06 - Agregar nuevo producto (la API responde con el id nuevo pero no lo guarda de verdad)
    @POST("products")
    fun createProduct(@Body product: ProductRequest): Call<ProductResponse>

    // US07 - Editar producto (la API responde 200 con el objeto, pero tampoco cambia nada real)
    @PUT("products/{id}")
    fun updateProduct(@Path("id") id: Int, @Body product: ProductRequest): Call<ProductResponse>

    // US08 - Eliminar producto. Uso ResponseBody porque solo me importa saber si
    // la respuesta fue exitosa (a veces el body viene vacío o "null" y Gson fallaría)
    @DELETE("products/{id}")
    fun deleteProduct(@Path("id") id: Int): Call<ResponseBody>
}
