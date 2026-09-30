package com.example.equipo_dinamita.vista

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.example.equipo_dinamita.R
import com.example.equipo_dinamita.config.RetrofitClient
import com.example.equipo_dinamita.config.SessionManager
import com.example.equipo_dinamita.dao.ApiService
import com.example.equipo_dinamita.modelo.LoginRequest
import com.example.equipo_dinamita.modelo.LoginResponse
import com.example.equipo_dinamita.modelo.User
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * MVC: VISTA (pantalla de login, US01)
 * Recibe usuario y contraseña, llama a la API y guarda la sesión con su rol.
 * Igual que HomeActivity, aquí la Activity también hace un poco de controlador
 * (maneja el clic y las llamadas del login).
 */
class LoginActivity : AppCompatActivity() {

    // vistas del formulario de login
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var progressLogin: ProgressBar
    private lateinit var rootView: View

    private val apiService by lazy { RetrofitClient.instance.create(ApiService::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // enlazo las vistas del layout con las variables
        rootView = findViewById(R.id.rootLogin)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressLogin = findViewById(R.id.progressLogin)

        // Si ya hay una sesión válida guardada, no tiene sentido pedir login de nuevo.
        if (SessionManager.isLoggedIn(this)) {
            goToHome()
            return
        }

        // al presionar INGRESAR se valida y se hace el login
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            // validación básica: no dejar campos vacíos
            if (username.isEmpty() || password.isEmpty()) {
                showAlert("Por favor complete todos los campos", isError = false)
                return@setOnClickListener
            }

            // Escenario 3 (US01): sin conectividad, se detiene ANTES de llamar a la API.
            if (!isNetworkAvailable()) {
                showAlert("Sin conexión a internet. Verifica tu red e inténtalo de nuevo.", isError = true)
                return@setOnClickListener
            }

            performLogin(username, password)
        }
    }

    // manda usuario y contraseña a POST /auth/login
    private fun performLogin(username: String, password: String) {
        setLoading(true)
        val request = LoginRequest(username, password)

        apiService.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                val token = response.body()?.token

                // Escenario 2 (US01): credenciales incorrectas -> 401/400 u otro código de error.
                if (!response.isSuccessful || token.isNullOrEmpty()) {
                    setLoading(false)
                    showAlert("Usuario o contraseña inválidos", isError = true)
                    return
                }

                // Escenario 1 (US01): token recibido -> se descarga la info del usuario
                // para mapear su rol antes de continuar.
                fetchUserAndProceed(token, username)
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                setLoading(false)
                showAlert("Error de conexión: ${t.message}", isError = true)
            }
        })
    }

    /**
     * La FakeStoreAPI no incluye el ID de usuario en la respuesta de login,
     * así que se descarga la lista de usuarios (recurso /users) y se ubica
     * el que coincide con el username para obtener su ID real y mapear el rol.
     */
    private fun fetchUserAndProceed(token: String, username: String) {
        apiService.getUsers().enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                setLoading(false)

                val matchedUser = response.body()?.firstOrNull {
                    it.username.equals(username, ignoreCase = true)
                }

                val userId = matchedUser?.id ?: -1
                val role = SessionManager.mapRoleFromUserId(userId)

                SessionManager.saveSession(
                    context = this@LoginActivity,
                    token = token,
                    userId = userId,
                    username = username,
                    role = role
                )

                goToHome()
            }

            override fun onFailure(call: Call<List<User>>, t: Throwable) {
                setLoading(false)
                // El login ya fue exitoso; si falla solo la resolución del rol,
                // se deja pasar como CLIENTE en vez de bloquear al usuario.
                SessionManager.saveSession(
                    context = this@LoginActivity,
                    token = token,
                    userId = -1,
                    username = username,
                    role = "CLIENTE"
                )
                goToHome()
            }
        })
    }

    // pasa a la pantalla principal (catálogo)
    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        // Limpia el back-stack para que, ya autenticado, no se pueda volver
        // a esta pantalla de Login con "Atrás".
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // muestra/oculta el loader y bloquea el botón mientras se hace la petición
    private fun setLoading(isLoading: Boolean) {
        progressLogin.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !isLoading
        btnLogin.alpha = if (isLoading) 0.6f else 1f
    }

    /** Alerta visual: roja para errores (US01, Escenario 2), neutra para avisos. */
    private fun showAlert(message: String, isError: Boolean) {
        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
        if (isError) {
            snackbar.setBackgroundTint(Color.parseColor("#D32F2F"))
            snackbar.setTextColor(Color.WHITE)
        }
        snackbar.show()
    }

    // revisa si el teléfono tiene internet (wifi, datos o ethernet)
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}
