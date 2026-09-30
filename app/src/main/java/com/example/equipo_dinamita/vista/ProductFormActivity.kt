package com.example.equipo_dinamita.vista

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doAfterTextChanged
import com.example.equipo_dinamita.R
import com.example.equipo_dinamita.controlador.ProductController
import com.example.equipo_dinamita.controlador.ProductController.Campo
import com.example.equipo_dinamita.modelo.ProductRequest
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * MVC: VISTA
 *
 * US06 - Agregar nuevo producto y US07 - Editar producto.
 * Uso la misma pantalla para las dos cosas:
 *  - si NO recibe un id en el Intent -> modo "agregar" (POST /products)
 *  - si SÍ recibe un id (y los datos del producto) -> modo "editar" (PUT /products/{id})
 * Esta Activity solo pinta el formulario y recibe los clics; la validación y las
 * peticiones a la API las hace el ProductController.
 */
class ProductFormActivity : AppCompatActivity() {

    companion object {
        // llaves para pasar los datos del producto entre pantallas (Intent extras)
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_PRICE = "extra_price"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_IMAGE = "extra_image"
    }

    // el controlador se crea en onCreate porque necesita el contexto
    private lateinit var controller: ProductController

    // -1 significa que no hay producto, o sea que estoy en modo "agregar"
    private var productId: Int = -1
    private val modoEditar: Boolean
        get() = productId != -1

    // vistas del formulario
    private lateinit var rootView: View
    private lateinit var tilTitulo: TextInputLayout
    private lateinit var tilPrecio: TextInputLayout
    private lateinit var tilDescripcion: TextInputLayout
    private lateinit var tilImagen: TextInputLayout
    private lateinit var tilCategoria: TextInputLayout
    private lateinit var etTitulo: TextInputEditText
    private lateinit var etPrecio: TextInputEditText
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var etImagen: TextInputEditText
    private lateinit var etCategoria: TextInputEditText
    private lateinit var btnGuardar: Button
    private lateinit var progressForm: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_form)

        controller = ProductController(this)

        // US06 Escenario 3 y US07 Escenario 3: si el usuario no es Administrador
        // (por ejemplo entró forzando la navegación) se bloquea el acceso y
        // se manda de regreso al catálogo principal.
        if (!controller.esAdministrador()) {
            Toast.makeText(this, "No tienes permisos para esta acción", Toast.LENGTH_SHORT).show()
            redirigirAlCatalogo()
            return
        }

        // enlazo las vistas del layout con las variables
        rootView = findViewById(R.id.rootForm)
        tilTitulo = findViewById(R.id.tilTitulo)
        tilPrecio = findViewById(R.id.tilPrecio)
        tilDescripcion = findViewById(R.id.tilDescripcion)
        tilImagen = findViewById(R.id.tilImagen)
        tilCategoria = findViewById(R.id.tilCategoria)
        etTitulo = findViewById(R.id.etTitulo)
        etPrecio = findViewById(R.id.etPrecio)
        etDescripcion = findViewById(R.id.etDescripcion)
        etImagen = findViewById(R.id.etImagen)
        etCategoria = findViewById(R.id.etCategoria)
        btnGuardar = findViewById(R.id.btnGuardar)
        progressForm = findViewById(R.id.progressForm)

        val toolbar = findViewById<Toolbar>(R.id.toolbarForm)
        toolbar.setNavigationOnClickListener { finish() }

        // ¿me mandaron un id? entonces es edición
        productId = intent.getIntExtra(EXTRA_ID, -1)
        if (modoEditar) {
            toolbar.title = "Editar producto"
            btnGuardar.text = "GUARDAR CAMBIOS"
            precargarFormulario()
        } else {
            toolbar.title = "Agregar producto"
            btnGuardar.text = "GUARDAR PRODUCTO"
        }

        // cuando la persona corrige un campo le quito el error rojo
        etTitulo.doAfterTextChanged { tilTitulo.error = null }
        etPrecio.doAfterTextChanged { tilPrecio.error = null }
        etDescripcion.doAfterTextChanged { tilDescripcion.error = null }
        etImagen.doAfterTextChanged { tilImagen.error = null }
        etCategoria.doAfterTextChanged { tilCategoria.error = null }

        btnGuardar.setOnClickListener { onGuardarClick() }
    }

    /** US07 Escenario 2: los campos aparecen ya llenos con la información actual del artículo. */
    private fun precargarFormulario() {
        etTitulo.setText(intent.getStringExtra(EXTRA_TITLE) ?: "")
        etPrecio.setText(intent.getDoubleExtra(EXTRA_PRICE, 0.0).toString())
        etDescripcion.setText(intent.getStringExtra(EXTRA_DESCRIPTION) ?: "")
        etImagen.setText(intent.getStringExtra(EXTRA_IMAGE) ?: "")
        etCategoria.setText(intent.getStringExtra(EXTRA_CATEGORY) ?: "")
    }

    private fun onGuardarClick() {
        val titulo = etTitulo.text.toString()
        val precio = etPrecio.text.toString()
        val descripcion = etDescripcion.text.toString()
        val imagen = etImagen.text.toString()
        val categoria = etCategoria.text.toString()

        // US06 Escenario 2: primero valido LOCAL. Si hay errores no se hace ninguna petición
        val errores = controller.validar(titulo, precio, descripcion, imagen, categoria)
        if (errores.isNotEmpty()) {
            mostrarErrores(errores)
            return
        }

        // ya validé el precio, por eso aquí puedo convertir sin miedo
        val request = ProductRequest(
            title = titulo.trim(),
            price = precio.trim().toDouble(),
            description = descripcion.trim(),
            image = imagen.trim(),
            category = categoria.trim()
        )

        // US07: bloqueo el botón y muestro el loader para evitar envíos duplicados
        setCargando(true)

        if (modoEditar) {
            controller.actualizarProducto(
                id = productId,
                request = request,
                onExito = { onActualizado(request) },
                onError = { mensaje -> onErrorPeticion(mensaje) }
            )
        } else {
            controller.crearProducto(
                request = request,
                onExito = { nuevoId -> onCreado(nuevoId) },
                onError = { mensaje -> onErrorPeticion(mensaje) }
            )
        }
    }

    /** US06 Escenario 1: alerta de confirmación con el nuevo ID y limpio el formulario. */
    private fun onCreado(nuevoId: Int) {
        // si la Activity ya se cerró mientras llegaba la respuesta, no hago nada
        if (isFinishing || isDestroyed) return
        setCargando(false)

        AlertDialog.Builder(this)
            .setTitle("Producto creado")
            .setMessage("El producto se registró correctamente (simulación).\n\nID generado: $nuevoId")
            .setPositiveButton("Aceptar", null)
            .show()

        limpiarFormulario()
    }

    /**
     * US07 Escenario 1: mensaje de simulación, se cierra el formulario y le regreso
     * los datos nuevos a la pantalla de detalle (la API no cambia nada de verdad,
     * por eso el detalle usa estos datos locales para reflejar el cambio).
     */
    private fun onActualizado(request: ProductRequest) {
        if (isFinishing || isDestroyed) return

        Toast.makeText(this, "Producto actualizado (Simulación)", Toast.LENGTH_SHORT).show()

        val resultado = Intent().apply {
            putExtra(EXTRA_ID, productId)
            putExtra(EXTRA_TITLE, request.title)
            putExtra(EXTRA_PRICE, request.price)
            putExtra(EXTRA_DESCRIPTION, request.description)
            putExtra(EXTRA_CATEGORY, request.category)
            putExtra(EXTRA_IMAGE, request.image)
        }
        setResult(RESULT_OK, resultado)
        finish()
    }

    private fun onErrorPeticion(mensaje: String) {
        if (isFinishing || isDestroyed) return
        setCargando(false)

        // Snackbar rojo igual que en el login
        Snackbar.make(rootView, mensaje, Snackbar.LENGTH_LONG)
            .setBackgroundTint(Color.parseColor("#D32F2F"))
            .setTextColor(Color.WHITE)
            .show()
    }

    /** Marca en rojo (con el error del TextInputLayout) cada campo que está mal. */
    private fun mostrarErrores(errores: Map<Campo, String>) {
        tilTitulo.error = errores[Campo.TITULO]
        tilPrecio.error = errores[Campo.PRECIO]
        tilDescripcion.error = errores[Campo.DESCRIPCION]
        tilImagen.error = errores[Campo.IMAGEN]
        tilCategoria.error = errores[Campo.CATEGORIA]

        // pongo el foco en el primer campo con error para que se vea rápido
        when {
            errores.containsKey(Campo.TITULO) -> etTitulo.requestFocus()
            errores.containsKey(Campo.PRECIO) -> etPrecio.requestFocus()
            errores.containsKey(Campo.DESCRIPCION) -> etDescripcion.requestFocus()
            errores.containsKey(Campo.IMAGEN) -> etImagen.requestFocus()
            errores.containsKey(Campo.CATEGORIA) -> etCategoria.requestFocus()
        }
    }

    private fun limpiarFormulario() {
        etTitulo.text?.clear()
        etPrecio.text?.clear()
        etDescripcion.text?.clear()
        etImagen.text?.clear()
        etCategoria.text?.clear()
        // al limpiar los textos los errores ya se quitan solos (doAfterTextChanged)
        etTitulo.requestFocus()
    }

    // prende/apaga el estado de "guardando..."
    private fun setCargando(cargando: Boolean) {
        progressForm.visibility = if (cargando) View.VISIBLE else View.GONE
        btnGuardar.isEnabled = !cargando
        btnGuardar.alpha = if (cargando) 0.6f else 1f
    }

    // regresa al catálogo principal (HomeActivity) limpiando lo que hay encima
    private fun redirigirAlCatalogo() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}
