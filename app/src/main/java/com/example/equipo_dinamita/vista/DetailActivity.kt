package com.example.equipo_dinamita.vista

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewStub
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.equipo_dinamita.R
import com.example.equipo_dinamita.config.RetrofitClient
import com.example.equipo_dinamita.config.SessionManager
import com.example.equipo_dinamita.controlador.ProductController
import com.example.equipo_dinamita.dao.ApiService
import com.example.equipo_dinamita.modelo.Product
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

/**
 * MVC: VISTA
 *
 * US05 - Ver detalle del producto con interfaz dinámica.
 * Consume GET /products/{id} y adapta los controles visibles según el rol
 * leído de la sesión local (SessionManager), nunca delegando esa decisión
 * a la Fake Store API.
 *
 * US07 (editar) y US08 (eliminar): los botones de esta pantalla llaman al
 * ProductController; aquí solo abro el formulario o muestro el diálogo de confirmación.
 */
class DetailActivity : AppCompatActivity() {

    companion object {
        // llave para recibir el id del producto que se tocó en el catálogo
        const val EXTRA_PRODUCT_ID = "extra_product_id"
    }

    private val apiService by lazy { RetrofitClient.instance.create(ApiService::class.java) }

    // controlador de US06/US07/US08 (MVC)
    private lateinit var controller: ProductController

    // producto que se está mostrando ahorita (lo actualizo cuando se edita)
    private var productoActual: Product? = null

    // el ViewStub solo se puede inflar UNA vez, con esta bandera evito inflarlo dos veces
    private var accionesAdminInfladas = false

    // US07: aquí recibo el resultado del formulario de edición.
    // Como la API no guarda los cambios de verdad, uso los datos que regresa el formulario
    // para refrescar esta pantalla ("refleja los cambios visualmente").
    private val editarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val data = resultado.data
        if (resultado.resultCode == RESULT_OK && data != null) {
            val actualizado = Product(
                id = data.getIntExtra(ProductFormActivity.EXTRA_ID, -1),
                title = data.getStringExtra(ProductFormActivity.EXTRA_TITLE) ?: "",
                price = data.getDoubleExtra(ProductFormActivity.EXTRA_PRICE, 0.0),
                description = data.getStringExtra(ProductFormActivity.EXTRA_DESCRIPTION) ?: "",
                category = data.getStringExtra(ProductFormActivity.EXTRA_CATEGORY) ?: "",
                image = data.getStringExtra(ProductFormActivity.EXTRA_IMAGE) ?: ""
            )
            productoActual = actualizado
            mostrarDatos(actualizado)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        controller = ProductController(this)

        // flecha de la toolbar: regresa a la pantalla anterior
        val toolbar = findViewById<Toolbar>(R.id.toolbarDetail)
        toolbar.setNavigationOnClickListener { finish() }

        // si no llegó un id válido no hay nada que mostrar
        val productId = intent.getIntExtra(EXTRA_PRODUCT_ID, -1)
        if (productId == -1) {
            showNotAvailableAndReturn()
            return
        }

        fetchProduct(productId)
    }

    /**
     * US05 - Escenario 3: si la solicitud falla o el producto no existe,
     * se muestra la alerta "Producto no disponible" y se regresa
     * automáticamente al catálogo general.
     */
    private fun fetchProduct(productId: Int) {
        apiService.getProductById(productId).enqueue(object : Callback<Product> {
            override fun onResponse(call: Call<Product>, response: Response<Product>) {
                val product = response.body()
                if (!response.isSuccessful || product == null) {
                    showNotAvailableAndReturn()
                    return
                }
                bindProduct(product)
            }

            override fun onFailure(call: Call<Product>, t: Throwable) {
                showNotAvailableAndReturn()
            }
        })
    }

    // se ejecuta cuando ya llegó el producto de la API
    private fun bindProduct(product: Product) {
        productoActual = product
        findViewById<ProgressBar>(R.id.progressDetail).visibility = View.GONE
        val content = findViewById<View>(R.id.layoutDetailContent)
        content.visibility = View.VISIBLE

        mostrarDatos(product)

        // US05 - Escenario 1 vs 2: el rol se lee estrictamente de la sesión
        // local. Para Cliente/Auditor el ViewStub nunca se infla, así que
        // los botones de Editar/Eliminar ni siquiera existen en la vista.
        val role = SessionManager.getRole(this) ?: "CLIENTE"
        if (role == "ADMINISTRADOR" && !accionesAdminInfladas) {
            inflateAdminActions()
        }
    }

    // pinta los datos del producto en pantalla (también lo uso después de editar)
    private fun mostrarDatos(product: Product) {
        findViewById<TextView>(R.id.tvDetailTitle).text = product.title
        findViewById<TextView>(R.id.tvDetailCategory).text = product.category
        findViewById<TextView>(R.id.tvDetailPrice).text =
            String.format(Locale.getDefault(), "$%.2f", product.price)
        findViewById<TextView>(R.id.tvDetailDescription).text = product.description

        // Glide descarga la imagen y la muestra (con imagen de carga y de error)
        Glide.with(this)
            .load(product.image)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .into(findViewById<ImageView>(R.id.ivDetailImage))
    }

    // infla los botones de Editar/Eliminar (solo se llama si es Administrador)
    private fun inflateAdminActions() {
        val stub = findViewById<ViewStub>(R.id.stubAdminActions)
        val inflated = stub.inflate()
        accionesAdminInfladas = true

        // US07: abrir el formulario de edición con los datos actuales
        inflated.findViewById<View>(R.id.btnEditProduct).setOnClickListener {
            productoActual?.let { abrirFormularioEditar(it) }
        }

        // US08: pedir confirmación antes de borrar
        inflated.findViewById<View>(R.id.btnDeleteProduct).setOnClickListener {
            productoActual?.let { confirmarEliminacion(it) }
        }
    }

    /** US07 - Escenario 2: mando los datos actuales para que el formulario salga pre-cargado. */
    private fun abrirFormularioEditar(product: Product) {
        // doble seguridad: aunque el botón solo existe para el admin, el controlador vuelve a revisar
        if (!controller.esAdministrador()) {
            Toast.makeText(this, "No tienes permisos para editar productos", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, ProductFormActivity::class.java).apply {
            putExtra(ProductFormActivity.EXTRA_ID, product.id)
            putExtra(ProductFormActivity.EXTRA_TITLE, product.title)
            putExtra(ProductFormActivity.EXTRA_PRICE, product.price)
            putExtra(ProductFormActivity.EXTRA_DESCRIPTION, product.description)
            putExtra(ProductFormActivity.EXTRA_CATEGORY, product.category)
            putExtra(ProductFormActivity.EXTRA_IMAGE, product.image)
        }
        editarLauncher.launch(intent)
    }

    /**
     * US08 - Escenarios 1 y 2: NUNCA se manda el DELETE con un solo toque.
     * Primero sale un AlertDialog; si le dan "Cancelar" solo se cierra y no pasa nada
     * (no hay petición de red y la vista queda igual).
     */
    private fun confirmarEliminacion(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Estás seguro de eliminar este producto?")
            .setPositiveButton("Eliminar") { _, _ -> eliminarProducto(product) }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // US08 - ya confirmado, se llama al controlador para hacer el DELETE
    private fun eliminarProducto(product: Product) {
        val btnEliminar = findViewById<View>(R.id.btnDeleteProduct)
        // desactivo el botón para que no manden el DELETE dos veces
        btnEliminar.isEnabled = false

        controller.eliminarProducto(
            id = product.id,
            onExito = {
                if (!isFinishing && !isDestroyed) {
                    // Escenario 1: mensaje de éxito y regreso al catálogo general
                    Toast.makeText(this, "Producto eliminado (Simulación)", Toast.LENGTH_SHORT).show()
                    finish()
                }
            },
            onError = { mensaje ->
                if (!isFinishing && !isDestroyed) {
                    btnEliminar.isEnabled = true
                    Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // alerta de "Producto no disponible" (US05) y regreso al catálogo
    private fun showNotAvailableAndReturn() {
        AlertDialog.Builder(this)
            .setTitle("Producto no disponible")
            .setMessage("No se pudo cargar la información de este producto.")
            .setCancelable(false)
            .setPositiveButton("Aceptar") { _, _ -> finish() }
            .show()
    }
}
