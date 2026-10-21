package com.cursosvirtuales.app.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class CursoRemoto(val codigo: String, val nombre: String, val creditos: Int,
    val semestre: Int, val cupo: Int, val inscritos: Int)
data class EstudianteRemoto(val codigo: String, val nombre: String, val correo: String,
    val cursos: List<String>)
data class NuevoCurso(val codigo: String, val nombre: String, val creditos: Int,
    val semestre: Int, val cupo: Int)
data class NuevoEstudiante(val codigo: String, val nombre: String, val correo: String)
data class NuevaInscripcion(val estudiante: String, val curso: String)
data class MensajeApi(val mensaje: String)

/** Contrato de GestionAcademicaJPA: rutas relativas a /academica/api/. */
interface AcademicaApi {
    @GET("cursos") suspend fun cursos(): List<CursoRemoto>
    @GET("estudiantes") suspend fun estudiantes(): List<EstudianteRemoto>
    @POST("cursos") suspend fun crearCurso(@Body curso: NuevoCurso): MensajeApi
    @POST("estudiantes") suspend fun crearEstudiante(@Body estudiante: NuevoEstudiante): MensajeApi
    @POST("inscripciones") suspend fun inscribir(@Body inscripcion: NuevaInscripcion): MensajeApi
}
