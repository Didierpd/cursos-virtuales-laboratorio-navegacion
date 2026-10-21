package com.cursosvirtuales.app.network

import com.google.gson.Gson
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Único punto de construcción del cliente y acceso a los servicios remotos. */
class AcademicaRepository(baseUrl: String) {
    private val api: AcademicaApi
    init {
        val url = baseUrl.trim().let { if (it.endsWith("/")) it else "$it/" }.toHttpUrlOrNull()
        require(url != null && url.username.isEmpty() && url.password.isEmpty() &&
            url.query == null && url.fragment == null) { "Escribe una URL HTTP o HTTPS válida sin credenciales." }
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS).retryOnConnectionFailure(false).build()
        api = Retrofit.Builder().baseUrl(url).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(AcademicaApi::class.java)
    }
    suspend fun cursos() = api.cursos()
    suspend fun estudiantes() = api.estudiantes()
    suspend fun crearCurso(curso: NuevoCurso) = api.crearCurso(curso)
    suspend fun crearEstudiante(estudiante: NuevoEstudiante) = api.crearEstudiante(estudiante)
    suspend fun inscribir(inscripcion: NuevaInscripcion) = api.inscribir(inscripcion)

    companion object {
        fun mensajeError(error: Exception): String = when (error) {
            is HttpException -> {
                val detalle = runCatching {
                    error.response()?.errorBody()?.use { Gson().fromJson(it.string(), MensajeApi::class.java).mensaje }
                }.getOrNull()
                "HTTP ${error.code()}: ${detalle ?: "El servidor rechazó la solicitud."}"
            }
            is IOException -> "No se pudo conectar. Revisa la URL, la red y que GlassFish esté iniciado. Si estabas registrando datos, consulta la lista antes de repetir el envío."
            is IllegalArgumentException -> error.message ?: "Revisa los datos ingresados."
            else -> "No se pudo interpretar la respuesta del servidor."
        }
    }
}
