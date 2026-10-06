package com.cursosvirtuales.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.cursosvirtuales.app.model.Role
import com.cursosvirtuales.app.model.User
import java.security.MessageDigest
import java.util.UUID

/**
 * Repositorio de autenticación, registro y de perfil de usuario.
 *
 * Los usuarios se guardan en SQLite (tabla USUARIO, ver AppDatabaseHelper),
 * no en memoria — persisten entre ejecuciones de la app. Debe llamarse
 * AuthRepository.init(context) una vez (por ejemplo en el onCreate de la
 * primera Activity que lo use) antes de usar cualquier otro método.
 */
object AuthRepository {

    private lateinit var dbHelper: AppDatabaseHelper
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        dbHelper = AppDatabaseHelper(context.applicationContext)
        initialized = true
    }

    /** Hash de contraseña con SHA-256. Nunca se persiste en texto plano. */
    fun hash(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    sealed class LoginResult {
        data class Success(val user: User) : LoginResult()
        object InvalidCredentials : LoginResult()
        object EmptyFields : LoginResult()
    }

    /** Verificación de credenciales (tarea 1.2 / 1.7: mensaje de error genérico). */
    fun login(username: String, password: String): LoginResult {
        if (username.isBlank() || password.isBlank()) return LoginResult.EmptyFields
        val user = findByUsername(username) ?: return LoginResult.InvalidCredentials
        return if (user.passwordHash == hash(password)) {
            LoginResult.Success(user)
        } else {
            LoginResult.InvalidCredentials
        }
    }

    sealed class RegisterResult {
        data class Success(val user: User) : RegisterResult()
        object UsernameTaken : RegisterResult()
        object EmptyFields : RegisterResult()
    }

    /** Registro de una cuenta nueva (Docente o Estudiante, a elección del formulario). */
    fun register(username: String, password: String, role: Role, fullName: String, email: String): RegisterResult {
        if (username.isBlank() || password.isBlank() || fullName.isBlank() || email.isBlank()) {
            return RegisterResult.EmptyFields
        }
        if (findByUsername(username) != null) {
            return RegisterResult.UsernameTaken
        }

        val user = User(
            id = "u-" + UUID.randomUUID().toString(),
            username = username,
            passwordHash = hash(password),
            role = role,
            fullName = fullName,
            email = email
        )

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COL_USUARIO_ID, user.id)
            put(AppDatabaseHelper.COL_USUARIO_USERNAME, user.username)
            put(AppDatabaseHelper.COL_USUARIO_PASSWORD_HASH, user.passwordHash)
            put(AppDatabaseHelper.COL_USUARIO_ROL, user.role.name)
            put(AppDatabaseHelper.COL_USUARIO_NOMBRE, user.fullName)
            put(AppDatabaseHelper.COL_USUARIO_EMAIL, user.email)
        }
        db.insert(AppDatabaseHelper.TABLA_USUARIO, null, values)
        db.close()
        return RegisterResult.Success(user)
    }

    fun findById(userId: String): User? = buscarUno(AppDatabaseHelper.COL_USUARIO_ID, userId)

    private fun findByUsername(username: String): User? =
        buscarUno(AppDatabaseHelper.COL_USUARIO_USERNAME, username)

    private fun buscarUno(columna: String, valor: String): User? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            AppDatabaseHelper.TABLA_USUARIO, null,
            "$columna = ?", arrayOf(valor),
            null, null, null
        )
        val user = if (cursor.moveToFirst()) cursorToUser(cursor) else null
        cursor.close()
        db.close()
        return user
    }

    private fun cursorToUser(cursor: Cursor): User {
        return User(
            id = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_ID)),
            username = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_USERNAME)),
            passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_PASSWORD_HASH)),
            role = Role.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_ROL))),
            fullName = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_NOMBRE)),
            email = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_EMAIL)),
            photoUri = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_FOTO_URI))
        )
    }

    /** Actualiza los datos de perfil validando obligatorios (tarea 2.4). */
    fun updateProfile(userId: String, fullName: String, email: String, photoUri: String?): Boolean {
        if (fullName.isBlank() || email.isBlank()) return false
        if (findById(userId) == null) return false

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COL_USUARIO_NOMBRE, fullName)
            put(AppDatabaseHelper.COL_USUARIO_EMAIL, email)
            photoUri?.let { put(AppDatabaseHelper.COL_USUARIO_FOTO_URI, it) }
        }
        val filas = db.update(
            AppDatabaseHelper.TABLA_USUARIO, values,
            "${AppDatabaseHelper.COL_USUARIO_ID} = ?", arrayOf(userId)
        )
        db.close()
        return filas > 0
    }

    sealed class PasswordChangeResult {
        object Success : PasswordChangeResult()
        object WrongCurrentPassword : PasswordChangeResult()
        object EmptyFields : PasswordChangeResult()
    }

    /** Cambio de contraseña desde el perfil (tarea 2.6). */
    fun updatePassword(userId: String, currentPassword: String, newPassword: String): PasswordChangeResult {
        if (currentPassword.isBlank() || newPassword.isBlank()) return PasswordChangeResult.EmptyFields
        val user = findById(userId) ?: return PasswordChangeResult.WrongCurrentPassword
        if (user.passwordHash != hash(currentPassword)) return PasswordChangeResult.WrongCurrentPassword

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COL_USUARIO_PASSWORD_HASH, hash(newPassword))
        }
        db.update(
            AppDatabaseHelper.TABLA_USUARIO, values,
            "${AppDatabaseHelper.COL_USUARIO_ID} = ?", arrayOf(userId)
        )
        db.close()
        return PasswordChangeResult.Success
    }
}
