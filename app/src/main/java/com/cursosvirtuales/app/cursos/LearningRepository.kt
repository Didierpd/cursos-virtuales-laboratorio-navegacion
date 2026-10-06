package com.cursosvirtuales.app.cursos

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.cursosvirtuales.app.data.AppDatabaseHelper
import com.cursosvirtuales.app.model.Role
import java.io.File
import java.util.UUID

/** SQLite and private file storage, shared by both roles on this device. */
class LearningRepository(private val context: Context) {
    data class Attachment(val name: String, val path: String, val mime: String)
    data class Submission(val id: Long, val student: String, val name: String, val date: Long,
                          val grade: Double?, val feedback: String, val files: List<Attachment>, val differenceMillis: Long)

    fun task(id: Long): CourseTask? = AppDatabaseHelper(context).use { helper ->
        helper.readableDatabase.rawQuery("SELECT * FROM TAREA WHERE _id=?", arrayOf(id.toString())).use { c ->
            if (!c.moveToFirst()) null else CourseTask(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getLong(4), c.getInt(5))
        }
    }

    fun submissions(task: CourseTask): List<Submission> {
        require(CourseAccess.allow(context, task.cursoId, false)) { "No tienes acceso a este curso." }
        val user = CourseAccess.user(context)!!
        val teacher = user.role == Role.DOCENTE
        return AppDatabaseHelper(context).use { helper ->
            val db = helper.readableDatabase
            val args = if (teacher) arrayOf(task.id.toString()) else arrayOf(task.id.toString(), user.id)
            db.rawQuery("SELECT E._id,E.ESTUDIANTE_ID,U.NOMBRE_COMPLETO,E.FECHA,E.NOTA,E.COMENTARIO,E.DIFERENCIA_MS FROM ENTREGA E JOIN USUARIO U ON U._id=E.ESTUDIANTE_ID WHERE E.TAREA_ID=?" +
                (if (teacher) "" else " AND E.ESTUDIANTE_ID=?") + " ORDER BY E.FECHA", args).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val id = c.getLong(0)
                        val files = db.rawQuery("SELECT NOMBRE,RUTA,MIME FROM ADJUNTO WHERE ENTREGA_ID=? ORDER BY _id", arrayOf(id.toString())).use { a ->
                            buildList { while (a.moveToNext()) add(Attachment(a.getString(0), a.getString(1), a.getString(2))) }
                        }
                        add(Submission(id, c.getString(1), c.getString(2), c.getLong(3), if(c.isNull(4)) null else c.getDouble(4), c.getString(5) ?: "", files, c.getLong(6)))
                    }
                }
            }
        }
    }

    fun acceptsLate(courseId: Long): Boolean {
        require(CourseAccess.allow(context,courseId,false)) { "No tienes acceso al curso." }
        return AppDatabaseHelper(context).use { helper ->
            helper.readableDatabase.rawQuery("SELECT ACEPTA_TARDIAS FROM CURSO WHERE _id=?",arrayOf(courseId.toString())).use { c ->
                c.moveToFirst() && c.getInt(0) == 1
            }
        }
    }

    fun setAcceptsLate(courseId: Long, accept: Boolean) {
        require(CourseAccess.allow(context,courseId,true)) { "Solo el docente de este curso puede cambiar la política." }
        AppDatabaseHelper(context).use { helper ->
            val values = ContentValues().apply { put("ACEPTA_TARDIAS",if(accept) 1 else 0) }
            check(helper.writableDatabase.update("CURSO",values,"_id=?",arrayOf(courseId.toString())) == 1)
        }
    }

    fun canSubmit(task: CourseTask): Boolean = CourseAccess.allow(context,task.cursoId,false) &&
        (System.currentTimeMillis() <= task.fechaLimite || acceptsLate(task.cursoId))

    fun timing(submission: Submission): String {
        val delta = submission.differenceMillis
        if (delta == 0L) return "A tiempo · En el límite del plazo"
        var seconds = (kotlin.math.abs(delta) + 999) / 1000
        val days = seconds / 86400; seconds %= 86400
        val hours = seconds / 3600; seconds %= 3600
        val minutes = seconds / 60; seconds %= 60
        val duration = buildList {
            if(days > 0) add("$days d")
            if(hours > 0) add("$hours h")
            if(minutes > 0) add("$minutes min")
            if(seconds > 0) add("$seconds s")
        }.joinToString(" ")
        return if(delta > 0) "Fuera de plazo · $duration de retraso" else "A tiempo · $duration antes del plazo"
    }

    fun displayName(uri: Uri): String = if (uri.scheme == "file") File(uri.path ?: "").name else context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    } ?: "Archivo"

    fun submit(taskId: Long, uris: List<Uri>) {
        val task = task(taskId) ?: error("La tarea ya no existe.")
        val user = CourseAccess.user(context) ?: error("Inicia sesión nuevamente.")
        require(user.role == Role.ESTUDIANTE && CourseAccess.allow(context, task.cursoId, false)) { "Debes estar inscrito en el curso." }
        require(canSubmit(task)) { "La fecha límite ya venció y el curso no acepta entregas tardías." }
        require(uris.isNotEmpty() && uris.size <= 10) { "Selecciona entre 1 y 10 archivos." }
        require(submissions(task).isEmpty()) { "Esta tarea ya fue entregada." }
        val folder = File(context.filesDir, "entregas").apply { mkdirs() }
        val copied = mutableListOf<File>()
        try {
            val attachments = uris.map { uri ->
                val dest = File(folder, UUID.randomUUID().toString())
                copied.add(dest)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            require(total <= 20L * 1024 * 1024) { "Cada archivo debe pesar como máximo 20 MB." }
                            output.write(buffer, 0, read)
                        }
                        require(total > 0) { "No se admiten archivos vacíos." }
                    }
                } ?: error("No se pudo leer el archivo.")
                Attachment(displayName(uri), dest.name, context.contentResolver.getType(uri) ?: "application/octet-stream")
            }
            // Authorize before opening the write transaction: other helpers use separate connections.
            require(CourseAccess.user(context)?.id == user.id && CourseAccess.allow(context, task.cursoId, false)) { "Tu sesión expiró o cambió." }
            // Recheck the deadline after copying; a failed copy never creates a delivery.
            AppDatabaseHelper(context).use { helper ->
                val db = helper.writableDatabase
                db.beginTransaction()
                try {
                    val now = System.currentTimeMillis()
                    val difference = db.rawQuery("SELECT T.FECHA_LIMITE,C.ACEPTA_TARDIAS FROM TAREA T JOIN CURSO C ON C._id=T.CURSO_ID WHERE T._id=?",arrayOf(taskId.toString())).use { c ->
                        check(c.moveToFirst()) { "La tarea ya no existe." }
                        val delta = now - c.getLong(0)
                        require(delta <= 0 || c.getInt(1) == 1) { "La fecha límite venció y el curso no acepta entregas tardías." }
                        delta
                    }
                    val values = ContentValues().apply { put("TAREA_ID",task.id); put("ESTUDIANTE_ID",user.id); put("FECHA",now); put("DIFERENCIA_MS",difference) }
                    val id = db.insertOrThrow("ENTREGA", null, values)
                    attachments.forEach { a ->
                        db.insertOrThrow("ADJUNTO",null,ContentValues().apply { put("ENTREGA_ID",id); put("NOMBRE",a.name); put("RUTA",a.path); put("MIME",a.mime) })
                    }
                    db.setTransactionSuccessful()
                } finally { db.endTransaction() }
            }
        } catch (e: Exception) {
            copied.forEach { it.delete() }
            throw e
        }
    }

    fun grade(taskId: Long, submissionId: Long, grade: Double, feedback: String) {
        val task = task(taskId) ?: error("La tarea ya no existe.")
        require(CourseAccess.allow(context, task.cursoId, true)) { "Solo el docente del curso puede calificar." }
        require(grade.isFinite() && grade >= 0 && grade <= task.puntaje) { "La nota debe estar entre 0 y ${task.puntaje}." }
        require(feedback.isNotBlank()) { "Escribe una retroalimentación." }
        AppDatabaseHelper(context).use { helper ->
            val values = ContentValues().apply { put("NOTA",grade); put("COMENTARIO",feedback.trim()) }
            check(helper.writableDatabase.update("ENTREGA",values,"_id=? AND TAREA_ID=?",arrayOf(submissionId.toString(), taskId.toString())) == 1) { "No se encontró la entrega." }
        }
    }

    companion object {
        @JvmStatic fun status(context: Context, task: CourseTask): String {
            val repo = LearningRepository(context)
            val user = CourseAccess.user(context) ?: return "Sesión expirada"
            val submissions = repo.submissions(task)
            if(user.role == Role.DOCENTE) return "${submissions.size} entrega(s) · ${submissions.count { it.grade != null }} calificada(s)"
            val delivery = submissions.firstOrNull()
            return when {
                delivery?.grade != null -> "Calificada: ${delivery.grade} / ${task.puntaje}"
                delivery != null -> "Entregada · ${repo.timing(delivery)} · Pendiente de calificación"
                System.currentTimeMillis() > task.fechaLimite -> if(repo.acceptsLate(task.cursoId)) "Vencida · Se permite entrega tardía" else "Vencida · Sin entregar"
                else -> "Pendiente · Vigente"
            }
        }
    }
}
