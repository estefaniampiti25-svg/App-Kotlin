package com.example.equipo_dinamita.config

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Envoltorio sobre EncryptedSharedPreferences (Jetpack Security) para
 * persistir el token de sesión y los datos de rol usando el almacenamiento
 * seguro nativo del dispositivo, en vez de SharedPreferences en texto plano
 * (requisito de negocio de la US01).
 *
 * MVC: lo uso como apoyo del controlador para saber quién inició sesión y con qué rol
 * (también es lo que valido en US06, US07 y US08 para dejar pasar solo al Administrador).
 */
object SessionManager {

    // nombres del archivo y de las llaves donde guardo los datos
    private const val PREFS_NAME = "secure_session_prefs"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_ROLE = "role"

    // crea (o abre) el archivo de preferencias cifrado
    private fun prefs(context: Context): SharedPreferences {
        // la llave maestra se guarda en el Keystore del teléfono
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // guarda todos los datos de la sesión al hacer login
    fun saveSession(context: Context, token: String, userId: Int, username: String, role: String) {
        prefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_ROLE, role)
            .apply()
    }

    // getters: regresan el dato guardado (o un valor por defecto si no hay)
    fun getToken(context: Context): String? = prefs(context).getString(KEY_TOKEN, null)

    fun getUserId(context: Context): Int = prefs(context).getInt(KEY_USER_ID, -1)

    fun getUsername(context: Context): String? = prefs(context).getString(KEY_USERNAME, null)

    fun getRole(context: Context): String? = prefs(context).getString(KEY_ROLE, null)

    // si hay token guardado entonces hay sesión iniciada
    fun isLoggedIn(context: Context): Boolean = getToken(context) != null

    /**
     * Limpieza profunda de sesión (US02, Escenario 3): elimina token, id,
     * username y rol del almacenamiento persistente por completo, no solo
     * variables en memoria.
     */
    fun clearSession(context: Context) {
        prefs(context).edit().clear().apply()
    }

    /**
     * Mapeo de roles por ID (regla de negocio de la US01):
     * IDs 1 y 2 -> ADMINISTRADOR, ID 3 -> AUDITOR, el resto -> CLIENTE.
     */
    fun mapRoleFromUserId(userId: Int): String {
        return when (userId) {
            1, 2 -> "ADMINISTRADOR"
            3 -> "AUDITOR"
            else -> "CLIENTE"
        }
    }
}
