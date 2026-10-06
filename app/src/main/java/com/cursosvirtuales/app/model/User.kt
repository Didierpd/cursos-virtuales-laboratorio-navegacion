package com.cursosvirtuales.app.model

/**
 * Roles soportados por la plataforma (ver Backlog: Docente / Estudiante).
 */
enum class Role {
    DOCENTE,
    ESTUDIANTE
}

/**
 * Modelo de usuario. En una implementación real, passwordHash y el resto de
 * datos vendrían del backend (ver tareas 1.2 y 2.3 del Sprint Backlog).
 */
data class User(
    val id: String,
    val username: String,
    var passwordHash: String,
    val role: Role,
    var fullName: String,
    var email: String,
    var photoUri: String? = null
)
