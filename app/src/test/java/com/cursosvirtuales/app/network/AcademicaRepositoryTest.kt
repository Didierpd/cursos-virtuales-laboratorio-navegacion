package com.cursosvirtuales.app.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import com.google.gson.JsonParser
import java.util.concurrent.TimeUnit

class AcademicaRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: AcademicaRepository
    @Before fun setup() {
        server = MockWebServer()
        server.start()
        repository = AcademicaRepository(server.url("/academica/api/").toString())
    }
    @After fun cleanup() { server.shutdown() }
    private fun json(body: String, status: Int = 200) {
        server.enqueue(MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body))
    }
    @Test fun consultaCursosConRutaYDeserializacion() = runBlocking {
        json("""[{"codigo":"REST1","nombre":"Redes","creditos":3,"semestre":2,"cupo":20,"inscritos":1}]""")
        assertEquals("Redes", repository.cursos().single().nombre)
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("GET", request.method)
        assertEquals("/academica/api/cursos", request.path)
    }
    @Test fun consultaEstudiantesConInscripciones() = runBlocking {
        json("""[{"codigo":"E1","nombre":"Ana","correo":"ana@example.com","cursos":["REST1"]}]""")
        assertEquals(listOf("REST1"), repository.estudiantes().single().cursos)
        assertEquals("/academica/api/estudiantes", server.takeRequest(2, TimeUnit.SECONDS)!!.path)
    }
    @Test fun postEnviaJsonYReconoce201() = runBlocking {
        json("""{"mensaje":"Curso registrado."}""", 201)
        assertEquals("Curso registrado.", repository.crearCurso(NuevoCurso("R1", "Redes", 3, 2, 20)).mensaje)
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("POST", request.method)
        assertEquals("/academica/api/cursos", request.path)
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals(20, body.get("cupo").asInt)
        assertEquals("R1", body.get("codigo").asString)
    }
    @Test fun postEstudianteEInscripcion() = runBlocking {
        json("""{"mensaje":"Estudiante registrado."}""", 201)
        repository.crearEstudiante(NuevoEstudiante("E1", "Ana", "ana@example.com"))
        assertEquals("/academica/api/estudiantes", server.takeRequest(2, TimeUnit.SECONDS)!!.path)
        json("""{"mensaje":"Inscripción registrada."}""", 201)
        repository.inscribir(NuevaInscripcion("E1", "R1"))
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("/academica/api/inscripciones", request.path)
        val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals("E1", body.get("estudiante").asString)
        assertEquals("R1", body.get("curso").asString)
    }
    @Test fun errorHttpMuestraMensajeDelBackend() = runBlocking {
        json("""{"mensaje":"El curso no tiene cupos disponibles."}""", 400)
        try {
            repository.inscribir(NuevaInscripcion("E1", "R1"))
            fail("Debe rechazar la inscripción")
        } catch (ex: HttpException) {
            assertEquals("HTTP 400: El curso no tiene cupos disponibles.", AcademicaRepository.mensajeError(ex))
        }
    }
    @Test fun rechazaUrlInvalida() {
        try { AcademicaRepository("sin-servidor"); fail("Debe validar URL") }
        catch (ex: IllegalArgumentException) { assertTrue(ex.message!!.contains("URL")) }
    }
}
