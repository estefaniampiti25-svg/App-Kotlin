package com.example.equipo_dinamita.controlador

import android.content.Context
import android.util.Patterns
import com.example.equipo_dinamita.config.RetrofitClient
import com.example.equipo_dinamita.config.SessionManager
import com.example.equipo_dinamita.dao.ApiService
import com.example.equipo_dinamita.modelo.ProductRequest
import com.example.equipo_dinamita.modelo.ProductResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * MVC: CONTROLADOR
 *
 * Aquí puse la lógica de US06 (agregar), US07 (editar) y US08 (eliminar) para que
 * las Activities (la VISTA) solo se encarguen de mostrar cosas y recibir los clics.
 * El controlador:
 *  1. revisa que el usuario sea Administrador (seguridad a nivel de código),
 *  2. valida los datos del formulario ANTES de hacer cualquier petición,
 *  3. le pide los datos al MODELO/API (ApiService) y avisa a la vista si salió bien o mal.
 */
class ProductController(private val context: Context) {

    // campos del formulario, los uso para saber cuál marcar en rojo
    enum class Campo { TITULO, PRECIO, DESCRIPCION, IMAGEN, CATEGORIA }

    private val apiService by lazy { RetrofitClient.instance.create(ApiService::class.java) }

    // El rol se lee siempre de la sesión local (SessionManager), nunca de la API
    fun esAdministrador(): Boolean {
        return SessionManager.isLoggedIn(context) &&
                SessionManager.getRole(context) == "ADMINISTRADOR"
    }

    /**
     * US06 Escenario 2 / US07 regla de negocio: validación local.
     * Regresa un mapa con los campos que están mal y su mensaje.
     * Si el mapa viene vacío significa que todo está correcto.
     */
    fun validar(
        titulo: String,
        precio: String,
        descripcion: String,
        imagen: String,
        categoria: String
    ): Map<Campo, String> {
        val errores = mutableMapOf<Campo, String>()

        // campos de texto que no pueden ir vacíos
        if (titulo.isBlank()) errores[Campo.TITULO] = "El título es obligatorio"
        if (descripcion.isBlank()) errores[Campo.DESCRIPCION] = "La descripción es obligatoria"
        if (categoria.isBlank()) errores[Campo.CATEGORIA] = "La categoría es obligatoria"

        // el precio tiene que ser un número (si escriben letras, toDoubleOrNull regresa null)
        val precioNumero = precio.trim().toDoubleOrNull()
        if (precio.isBlank()) {
            errores[Campo.PRECIO] = "El precio es obligatorio"
        } else if (precioNumero == null || !precioNumero.isFinite() || precioNumero <= 0) {
            errores[Campo.PRECIO] = "Ingresa un precio numérico mayor a 0"
        }

        // la imagen tiene que ser una URL con formato correcto (http o https)
        val url = imagen.trim()
        if (url.isEmpty()) {
            errores[Campo.IMAGEN] = "La URL de la imagen es obligatoria"
        } else if (!(url.startsWith("http://") || url.startsWith("https://")) ||
            !Patterns.WEB_URL.matcher(url).matches()
        ) {
            errores[Campo.IMAGEN] = "La URL no tiene un formato válido (ej. https://...)"
        }

        return errores
    }

    /** US06 - POST /products. Si sale bien regreso el ID que generó la API. */
    fun crearProducto(
        request: ProductRequest,
        onExito: (nuevoId: Int) -> Unit,
        onError: (mensaje: String) -> Unit
    ) {
        // un Cliente o Auditor nunca debe llegar a la petición de red
        if (!esAdministrador()) {
            onError("No tienes permisos para agregar productos")
            return
        }

        apiService.createProduct(request).enqueue(object : Callback<ProductResponse> {
            override fun onResponse(call: Call<ProductResponse>, response: Response<ProductResponse>) {
                if (response.isSuccessful) {
                    onExito(response.body()?.id ?: 0)
                } else {
                    onError("Error del servidor (código ${response.code()})")
                }
            }

            override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                onError("Error de red: ${t.message}")
            }
        })
    }

    /** US07 - PUT /products/{id}. La API responde 200 pero no cambia nada real (simulación). */
    fun actualizarProducto(
        id: Int,
        request: ProductRequest,
        onExito: () -> Unit,
        onError: (mensaje: String) -> Unit
    ) {
        // US07 Escenario 3: se bloquea la solicitud si el rol no es Administrador
        if (!esAdministrador()) {
            onError("No tienes permisos para editar productos")
            return
        }

        apiService.updateProduct(id, request).enqueue(object : Callback<ProductResponse> {
            override fun onResponse(call: Call<ProductResponse>, response: Response<ProductResponse>) {
                if (response.isSuccessful) {
                    onExito()
                } else {
                    onError("Error del servidor (código ${response.code()})")
                }
            }

            override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                onError("Error de red: ${t.message}")
            }
        })
    }

    /**
     * US08 - DELETE /products/{id}.
     * La vista es la que pide la confirmación con el AlertDialog; aquí solo
     * ejecuto el borrado. Con que la respuesta sea exitosa ya cuenta como correcto,
     * aunque después el GET siga mostrando el producto (es una API de mentiras).
     */
    fun eliminarProducto(
        id: Int,
        onExito: () -> Unit,
        onError: (mensaje: String) -> Unit
    ) {
        // US08 Escenario 3: bloqueo a nivel de código para Cliente/Auditor
        if (!esAdministrador()) {
            onError("No tienes permisos para eliminar productos")
            return
        }

        apiService.deleteProduct(id).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    onExito()
                } else {
                    onError("Error del servidor (código ${response.code()})")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onError("Error de red: ${t.message}")
            }
        })
    }
}
