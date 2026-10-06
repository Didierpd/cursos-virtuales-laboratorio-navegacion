package com.cursosvirtuales.app.cursos

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.cursosvirtuales.app.data.AppDatabaseHelper
import java.io.File
import java.util.UUID

/** HU-12. Guides and attached resources belong to a course and survive restarts. */
class LaboratoryRepository(private val context: Context) {
    data class Laboratory(val id: Long, val courseId: Long, val title: String, val objective: String,
                          val guide: String, val resources: List<LearningRepository.Attachment>)
    fun list(courseId: Long): List<Laboratory> {
        require(CourseAccess.allow(context,courseId,false)) { "No tienes acceso al laboratorio de este curso." }
        return AppDatabaseHelper(context).use { helper ->
            val db = helper.readableDatabase
            db.rawQuery("SELECT _id,TITULO,OBJETIVO,GUIA FROM LABORATORIO WHERE CURSO_ID=? ORDER BY _id",arrayOf(courseId.toString())).use { c ->
                buildList { while(c.moveToNext()) {
                    val id = c.getLong(0)
                    val files = db.rawQuery("SELECT NOMBRE,RUTA,MIME FROM LAB_RECURSO WHERE LAB_ID=? ORDER BY _id",arrayOf(id.toString())).use { a ->
                        buildList { while(a.moveToNext()) add(LearningRepository.Attachment(a.getString(0),a.getString(1),a.getString(2))) }
                    }
                    add(Laboratory(id,courseId,c.getString(1),c.getString(2),c.getString(3),files))
                } }
            }
        }
    }
    fun create(courseId: Long, title: String, objective: String, guide: String, uris: List<Uri>): Long {
        val userId = CourseAccess.user(context)?.id
        require(userId != null && CourseAccess.allow(context,courseId,true)) { "Solo el docente del curso puede crear prácticas." }
        require(title.isNotBlank() && objective.isNotBlank() && guide.isNotBlank()) { "Completa el título, el objetivo y la guía de la práctica." }
        require(uris.size <= 10) { "Puedes adjuntar como máximo 10 recursos." }
        val folder = File(context.filesDir,"laboratorios").apply { mkdirs() }
        val copied = mutableListOf<File>()
        try {
            val resources = uris.distinct().map { uri ->
                val dest = File(folder,UUID.randomUUID().toString()); copied.add(dest)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output ->
                        val buffer=ByteArray(8192); var total=0L
                        while(true) {
                            val read=input.read(buffer); if(read < 0) break
                            total += read
                            require(total <= 20L*1024*1024) { "Cada recurso debe pesar como máximo 20 MB." }
                            output.write(buffer,0,read)
                        }
                        require(total > 0) { "No se admiten recursos vacíos." }
                    }
                } ?: error("No se pudo leer un recurso.")
                LearningRepository.Attachment(LearningRepository(context).displayName(uri),dest.name,context.contentResolver.getType(uri) ?: "application/octet-stream")
            }
            require(CourseAccess.user(context)?.id == userId && CourseAccess.allow(context,courseId,true)) { "Tu sesión expiró o cambió." }
            return AppDatabaseHelper(context).use { helper ->
                val db=helper.writableDatabase
                db.beginTransaction()
                try {
                    // Recheck ownership using the same connection inside the transaction.
                    db.rawQuery("SELECT _id FROM CURSO WHERE _id=? AND DOCENTE_ID=?",arrayOf(courseId.toString(),userId)).use { c ->
                        require(c.moveToFirst()) { "El curso ya no está disponible." }
                    }
                    val id=db.insertOrThrow("LABORATORIO",null,ContentValues().apply {
                        put("CURSO_ID",courseId); put("TITULO",title.trim()); put("OBJETIVO",objective.trim()); put("GUIA",guide.trim())
                    })
                    resources.forEach { a -> db.insertOrThrow("LAB_RECURSO",null,ContentValues().apply {
                        put("LAB_ID",id); put("NOMBRE",a.name); put("RUTA",a.path); put("MIME",a.mime)
                    }) }
                    db.setTransactionSuccessful(); id
                } finally { db.endTransaction() }
            }
        } catch(e: Exception) { copied.forEach { it.delete() }; throw e }
    }
}
