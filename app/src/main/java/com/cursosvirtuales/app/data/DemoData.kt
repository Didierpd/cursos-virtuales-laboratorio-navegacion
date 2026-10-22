package com.cursosvirtuales.app.data

import android.content.Context
import com.cursosvirtuales.app.cursos.*
import com.cursosvirtuales.app.talleres.encomiendas.*

/** Datos opcionales de exposición; añade ejemplos sin borrar registros existentes. */
object DemoData {
    fun prepare(context: Context) {
        require(SessionManager(context).currentUserId() == "u-docente-1") { "Usa la cuenta docente de demostración." }
        val courses = CourseRepository(context)
        val title = "Demo · Desarrollo de aplicaciones móviles"
        val courseId = courses.listarPorDocente("u-docente-1").firstOrNull { it.nombre == title }?.id
            ?: courses.insertar(Course(-1, title, "Curso de exposición: interfaces, SQLite y servicios REST.",
                System.currentTimeMillis(), System.currentTimeMillis() + 90L * 86400000, "u-docente-1"))
        check(courseId > 0) { "No se pudo preparar el curso." }
        val units = UnitRepository(context)
        if (units.listarPorCurso(courseId).none { it.nombre == "Servicios REST con Retrofit" }) {
            units.insertar(courseId, "Servicios REST con Retrofit", "Consultar y registrar cursos desde Servicios REST en el menú principal.")
        }
        val tasks = TaskRepository(context)
        if (tasks.listarPorCurso(courseId).none { it.titulo == "Taller de integración Android" }) {
            tasks.insertar(CourseTask(-1, courseId, "Taller de integración Android",
                "Presentar GET y POST con Retrofit y adjuntar evidencias de SQLite y ConstraintLayout.",
                System.currentTimeMillis() + 30L * 86400000, 100))
        }
        EnrollmentRepository(context).inscribir(courseId, "u-estudiante-1")
        val labs = LaboratoryRepository(context)
        if (labs.list(courseId).none { it.title == "SQLite: registro y consulta" }) {
            labs.create(courseId, "SQLite: registro y consulta", "Comprobar persistencia local y búsqueda por guía.",
                "1. Abre Encomiendas desde el menú principal.\n2. Registra un envío.\n3. Consulta por guía o documento.\n4. Reinicia la app y verifica que se conserva.", emptyList())
        }
        val helper = EncomiendasDbHelper(context)
        try {
            if (helper.buscarEncomiendas("ENC-DEMO-001").isEmpty()) {
                val sender = Remitente("Ana Docente", "100100", "3001234567", "Campus central", "Bogota")
                val recipient = Destinatario("Luis Estudiante", "200200", "3011234567", "Sede norte", "Bogota")
                helper.insertarEncomienda(sender, recipient, Encomienda("ENC-DEMO-001", "Material de laboratorio", 1.5,
                    50000.0, "Normal", "Demostración", sender, recipient))
            }
        } finally { helper.close() }
    }
}
