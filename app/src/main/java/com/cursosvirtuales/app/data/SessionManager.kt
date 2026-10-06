package com.cursosvirtuales.app.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Maneja sesión/token y expiración por inactividad (tarea 1.4)
 * y el cierre de sesión invalidando el token (tarea 1.5).
 *
 * NOTA: esta es una simulación local con SharedPreferences para fines de
 * demostración del flujo de UI. En producción el token lo emite el backend
 * (tarea 1.2) y debe viajar en cada petición HTTP (ej. header Authorization).
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "session_prefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_LAST_ACTIVITY = "last_activity"

        /** Sesión expira tras 15 minutos sin actividad. */
        const val INACTIVITY_TIMEOUT_MS = 15 * 60 * 1000L
    }

    fun startSession(userId: String) {
        val token = UUID.randomUUID().toString()
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
            .apply()
    }

    /** Debe llamarse en cada pantalla protegida para renovar la ventana de inactividad. */
    fun touch() {
        if (prefs.contains(KEY_TOKEN)) {
            prefs.edit().putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
        }
    }

    /** true si hay un token válido y no ha expirado por inactividad. */
    fun isSessionValid(): Boolean {
        val token = prefs.getString(KEY_TOKEN, null) ?: return false
        val lastActivity = prefs.getLong(KEY_LAST_ACTIVITY, 0L)
        val expired = System.currentTimeMillis() - lastActivity > INACTIVITY_TIMEOUT_MS
        if (expired) {
            logout()
            return false
        }
        return token.isNotEmpty()
    }

    fun currentUserId(): String? = prefs.getString(KEY_USER_ID, null)

    /** Invalida el token y limpia la sesión (tarea 1.5). */
    fun logout() {
        prefs.edit().clear().apply()
    }
}
