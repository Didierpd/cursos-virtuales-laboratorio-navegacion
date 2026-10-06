package com.cursosvirtuales.app.cursos

import android.content.Context
import com.cursosvirtuales.app.data.AppDatabaseHelper
import com.cursosvirtuales.app.model.Role

/** HU-10. Never accepts a student ID from an Activity: the session owns the report. */
class GradebookRepository(private val context: Context) {
    data class GradeRow(val taskId: Long, val title: String, val maximum: Int,
                        val submitted: Boolean, val grade: Double?, val feedback: String)
    data class CourseGrades(val courseId: Long, val name: String, val rows: List<GradeRow>) {
        val earned: Double get() = rows.sumOf { it.grade ?: 0.0 }
        val available: Int get() = rows.sumOf { it.maximum }
        val gradedMaximum: Int get() = rows.filter { it.grade != null }.sumOf { it.maximum }
        val gradedCount: Int get() = rows.count { it.grade != null }
    }
    fun mine(): List<CourseGrades> {
        val user = CourseAccess.user(context)
        require(user?.role == Role.ESTUDIANTE) { "Inicia sesión como estudiante para consultar tus notas." }
        return AppDatabaseHelper(context).use { helper ->
            val db = helper.readableDatabase
            db.rawQuery("SELECT C._id,C.NOMBRE FROM CURSO C JOIN INSCRIPCION I ON I.CURSO_ID=C._id WHERE I.ESTUDIANTE_ID=? ORDER BY C.NOMBRE,C._id",arrayOf(user!!.id)).use { c ->
                buildList {
                    while(c.moveToNext()) {
                        val courseId = c.getLong(0)
                        val rows = db.rawQuery("SELECT T._id,T.TITULO,T.PUNTAJE,E._id,E.NOTA,E.COMENTARIO FROM TAREA T LEFT JOIN ENTREGA E ON E.TAREA_ID=T._id AND E.ESTUDIANTE_ID=? WHERE T.CURSO_ID=? ORDER BY T.FECHA_LIMITE,T._id",arrayOf(user.id,courseId.toString())).use { t ->
                            buildList { while(t.moveToNext()) add(GradeRow(t.getLong(0),t.getString(1),t.getInt(2),!t.isNull(3),if(t.isNull(4)) null else t.getDouble(4),t.getString(5) ?: "")) }
                        }
                        add(CourseGrades(courseId,c.getString(1),rows))
                    }
                }
            }
        }
    }
}
