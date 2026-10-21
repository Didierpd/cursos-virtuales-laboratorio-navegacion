package com.cursosvirtuales.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cursosvirtuales.app.data.SessionManager
import com.cursosvirtuales.app.databinding.ActivityRestServicesBinding
import com.cursosvirtuales.app.network.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class RestServicesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRestServicesBinding
    private var busy = false
    private val preferences by lazy { getSharedPreferences("rest_services", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRestServicesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "Servicios REST"
        binding.baseUrl.setText(preferences.getString("base_url", "http://10.0.2.2:8080/academica/api/"))
        binding.saveUrl.setOnClickListener {
            try {
                repository()
                preferences.edit().putString("base_url", value(binding.baseUrl)).apply()
                binding.status.text = "Conexión guardada. Consulta una lista para verificarla."
            } catch (ex: IllegalArgumentException) { binding.status.text = ex.message }
        }
        binding.loadCourses.setOnClickListener {
            request {
                binding.courses.text = "Sin resultados actuales."
                val items = it.cursos()
                binding.courses.text = if (items.isEmpty()) "No hay cursos registrados." else items.joinToString("\n\n") { c ->
                    "${c.codigo} · ${c.nombre}\n${c.creditos} créditos · semestre ${c.semestre} · inscritos ${c.inscritos}/${c.cupo}"
                }
                "Consulta completada: ${items.size} cursos."
            }
        }
        binding.loadStudents.setOnClickListener {
            request {
                binding.students.text = "Sin resultados actuales."
                val items = it.estudiantes()
                binding.students.text = if (items.isEmpty()) "No hay estudiantes registrados." else items.joinToString("\n\n") { e ->
                    "${e.codigo} · ${e.nombre}\n${e.correo}\nCursos: ${e.cursos.joinToString().ifEmpty { "ninguno" }}"
                }
                "Consulta completada: ${items.size} estudiantes."
            }
        }
        binding.createCourse.setOnClickListener {
            request {
                val course = NuevoCurso(code(binding.courseCode), required(binding.courseName),
                    number(binding.credits, 1..30), number(binding.semester, 1..20), number(binding.capacity, 1..1000))
                val result = it.crearCurso(course)
                listOf(binding.courseCode, binding.courseName, binding.credits, binding.semester, binding.capacity).forEach { f -> f.text.clear() }
                "${result.mensaje} Consulta cursos para ver el registro."
            }
        }
        binding.createStudent.setOnClickListener {
            request {
                val email = required(binding.studentEmail)
                require(android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { "Escribe un correo válido." }
                val result = it.crearEstudiante(NuevoEstudiante(code(binding.studentCode), required(binding.studentName), email))
                listOf(binding.studentCode, binding.studentName, binding.studentEmail).forEach { f -> f.text.clear() }
                "${result.mensaje} Consulta estudiantes para ver el registro."
            }
        }
        binding.enroll.setOnClickListener {
            request {
                val result = it.inscribir(NuevaInscripcion(code(binding.enrollmentStudent), code(binding.enrollmentCourse)))
                "${result.mensaje} Consulta estudiantes y cursos para ver los cambios."
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val session = SessionManager(this)
        if (!session.isSessionValid()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else session.touch()
    }

    private fun repository() = AcademicaRepository(value(binding.baseUrl))
    private fun value(field: EditText) = field.text.toString().trim()
    private fun required(field: EditText): String {
        val text = value(field)
        require(text.isNotEmpty()) { "Completa el campo: ${field.hint}." }
        return text
    }
    private fun code(field: EditText): String {
        val text = required(field).uppercase(java.util.Locale.ROOT)
        require(text.matches(Regex("[A-Z0-9_-]{1,20}"))) { "El código admite de 1 a 20 letras, números, guiones o guiones bajos." }
        return text
    }
    private fun number(field: EditText, range: IntRange): Int {
        val n = value(field).toIntOrNull()
        require(n != null && n in range) { "Revisa el campo: ${field.hint}." }
        return n
    }
    private fun request(action: suspend (AcademicaRepository) -> String) {
        if (busy) return
        lifecycleScope.launch {
            setBusy(true)
            binding.status.text = "Procesando solicitud…"
            try { binding.status.text = action(repository()) }
            catch (ex: CancellationException) { throw ex }
            catch (ex: Exception) { binding.status.text = AcademicaRepository.mensajeError(ex) }
            finally { setBusy(false) }
        }
    }
    private fun setBusy(value: Boolean) {
        busy = value
        binding.progress.visibility = if (value) View.VISIBLE else View.GONE
        listOf(binding.loadCourses, binding.loadStudents, binding.createCourse, binding.createStudent,
            binding.enroll, binding.saveUrl, binding.baseUrl, binding.courseCode, binding.courseName,
            binding.credits, binding.semester, binding.capacity, binding.studentCode, binding.studentName,
            binding.studentEmail, binding.enrollmentStudent, binding.enrollmentCourse).forEach { it.isEnabled = !value }
    }
}
