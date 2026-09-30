package com.example.equipo_dinamita.vista

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.equipo_dinamita.R
import com.example.equipo_dinamita.adaptador.ProductAdapter
import com.example.equipo_dinamita.config.RetrofitClient
import com.example.equipo_dinamita.config.SessionManager
import com.example.equipo_dinamita.dao.ApiService
import com.example.equipo_dinamita.modelo.Product
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * MVC: VISTA (pantalla principal / catálogo)
 *
 * Muestra el catálogo (US03), el filtro por categoría (US04) y el cierre de sesión (US02).
 * Nota: aquí la Activity también maneja los eventos y las llamadas del catálogo (hace de
 * vista + un poco de controlador). Lo nuevo de US06/US07/US08 sí lo separé en
 * ProductController.
 */
class HomeActivity : AppCompatActivity() {

    // adaptador del RecyclerView (lista de productos)
    private lateinit var productAdapter: ProductAdapter
    private val apiService by lazy { RetrofitClient.instance.create(ApiService::class.java) }

    // US04: categoría actualmente aplicada como filtro; null = catálogo general.
    private var selectedCategory: String? = null

    // vistas que uso en varios métodos
    private lateinit var rvProducts: RecyclerView
    private lateinit var progressProducts: ProgressBar
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var btnRetry: Button
    private lateinit var scrollCategories: View
    private lateinit var chipGroupCategories: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Si no hay sesión válida (p. ej. se llegó aquí por un back-stack viejo),
        // se manda de vuelta al Login en lugar de mostrar el catálogo.
        if (!SessionManager.isLoggedIn(this)) {
            goToLogin()
            return
        }

        // enlazo las vistas del layout
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvRole = findViewById<TextView>(R.id.tvRole)
        val cardTextContent = findViewById<MaterialCardView>(R.id.cardTextContent)
        val tvContent = findViewById<TextView>(R.id.tvContent)
        val btnAddProduct = findViewById<Button>(R.id.btnAddProduct)
        rvProducts = findViewById(R.id.rvProducts)
        progressProducts = findViewById(R.id.progressProducts)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        btnRetry = findViewById(R.id.btnRetry)
        scrollCategories = findViewById(R.id.scrollCategories)
        chipGroupCategories = findViewById(R.id.chipGroupCategories)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // datos de la sesión guardada
        val username = SessionManager.getUsername(this) ?: "Usuario"
        val role = SessionManager.getRole(this) ?: "CLIENTE"

        tvWelcome.text = "Bienvenido, $username"
        tvRole.text = "Rol: $role"
        tvRole.backgroundTintList = android.content.res.ColorStateList.valueOf(colorForRole(role))

        productAdapter = ProductAdapter { product ->
            // US05: al tocar un producto se navega al detalle dinámico según el rol.
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra(DetailActivity.EXTRA_PRODUCT_ID, product.id)
            startActivity(intent)
        }
        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = productAdapter

        // según el rol muestro un panel distinto
        when (role) {
            "ADMINISTRADOR" -> {
                cardTextContent.visibility = View.VISIBLE
                tvContent.text = "PANEL DE CONTROL ADMINISTRATIVO\n\n- Gestión de Usuarios\n- Reportes de Ventas\n- Configuración del Sistema"

                // US06: solo el Administrador ve el botón para agregar un producto nuevo
                btnAddProduct.visibility = View.VISIBLE
                btnAddProduct.setOnClickListener {
                    startActivity(Intent(this, ProductFormActivity::class.java))
                }
            }
            "AUDITOR" -> {
                cardTextContent.visibility = View.VISIBLE
                tvContent.text = "VISTA DE AUDITORÍA\n\n- Logs del Sistema\n- Historial de transacciones\n- Verificación de integridad"
            }
        }

        btnRetry.setOnClickListener { fetchProducts() }
        btnLogout.setOnClickListener { performLogout() }

        // cargo las categorías (chips) y el catálogo
        loadCategories()
        fetchProducts()
    }

    // regresa el color del badge según el rol (US01)
    private fun colorForRole(role: String): Int {
        val colorRes = when (role) {
            "ADMINISTRADOR" -> R.color.role_admin
            "AUDITOR" -> R.color.role_auditor
            else -> R.color.role_client
        }
        return ContextCompat.getColor(this, colorRes)
    }

    /**
     * US02 - Cierre de sesión y limpieza de credenciales:
     * Escenario 1: borra el token y redirige de inmediato al Login.
     * Escenario 2: limpia el back-stack para bloquear el regreso al catálogo.
     * Escenario 3: limpieza profunda de todo dato sensible (token, id, rol).
     */
    private fun performLogout() {
        SessionManager.clearSession(this)
        goToLogin()
    }

    // manda al Login y borra el historial de pantallas (para que "Atrás" no regrese aquí)
    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * US04 - Escenario 1: al inicializar la vista se consume
     * GET /products/categories y se muestran como chips de filtrado.
     * Si la petición falla, simplemente se oculta el filtro (el catálogo
     * general sigue funcionando con US03).
     */
    private fun loadCategories() {
        apiService.getCategories().enqueue(object : Callback<List<String>> {
            override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
                val categories = response.body()
                if (!response.isSuccessful || categories.isNullOrEmpty()) return

                scrollCategories.visibility = View.VISIBLE
                chipGroupCategories.removeAllViews()

                // primero el chip "Ver todos" y luego uno por cada categoría
                val chipTodos = createChip("Ver todos", isCheckedByDefault = true)
                chipGroupCategories.addView(chipTodos)
                categories.forEach { category ->
                    chipGroupCategories.addView(createChip(category))
                }

                // cuando eligen un chip, guardo la categoría y vuelvo a pedir los productos
                chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
                    val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
                    val chip = group.findViewById<Chip>(checkedId)
                    val category = chip.tag as? String
                    selectedCategory = category
                    fetchProducts()
                }
            }

            override fun onFailure(call: Call<List<String>>, t: Throwable) {
                // El filtro no es crítico para US03; se deja oculto si falla.
            }
        })
    }

    // crea un chip de categoría
    private fun createChip(label: String, isCheckedByDefault: Boolean = false): Chip {
        val chip = Chip(this)
        chip.text = label
        chip.isCheckable = true
        chip.isChecked = isCheckedByDefault
        // "Ver todos" se identifica con tag null -> catálogo general (US04, Escenario 3).
        chip.tag = if (label == "Ver todos") null else label
        return chip
    }

    /**
     * US03 (catálogo general) + US04 (catálogo filtrado): decide qué endpoint
     * consumir según si hay una categoría seleccionada.
     */
    private fun fetchProducts() {
        // muestro el loader y escondo lo demás
        progressProducts.visibility = View.VISIBLE
        rvProducts.visibility = View.GONE
        layoutEmptyState.visibility = View.GONE
        // US04 - Gestión de memoria: se limpia el arreglo anterior antes de
        // que llegue la nueva respuesta, para no mostrar datos incorrectos.
        productAdapter.submitList(emptyList())

        val call = selectedCategory?.let { apiService.getProductsByCategory(it) }
            ?: apiService.getAllProducts()

        call.enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                progressProducts.visibility = View.GONE
                val products = response.body()

                if (response.isSuccessful && !products.isNullOrEmpty()) {
                    rvProducts.visibility = View.VISIBLE
                    productAdapter.submitList(products)
                } else if (response.isSuccessful) {
                    showEmptyState("No hay productos disponibles por el momento.", showRetry = false)
                } else {
                    showEmptyState("Error al cargar productos (código ${response.code()}).", showRetry = true)
                }
            }

            override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                progressProducts.visibility = View.GONE
                // US03 - Escenario 3: error de conexión con botón para reintentar.
                showEmptyState("Error de red: ${t.message}", showRetry = true)
            }
        })
    }

    // muestra el mensaje de "vacío" o de error (con o sin botón de reintentar)
    private fun showEmptyState(message: String, showRetry: Boolean) {
        layoutEmptyState.visibility = View.VISIBLE
        tvEmptyState.text = message
        btnRetry.visibility = if (showRetry) View.VISIBLE else View.GONE
    }
}
