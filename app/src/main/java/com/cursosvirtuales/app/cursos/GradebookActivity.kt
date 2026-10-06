package com.cursosvirtuales.app.cursos

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat

class GradebookActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); title = "Mis calificaciones" }
    override fun onResume() {
        super.onResume()
        val courses = try { GradebookRepository(this).mine() } catch(e: IllegalArgumentException) {
            Toast.makeText(this,e.message,Toast.LENGTH_LONG).show(); finish(); return
        }
        val form = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,16,24,24) }
        setContentView(ScrollView(this).apply { addView(form) })
        fun label(value: String, size: Float=16f) { form.addView(TextView(this).apply { text=value; textSize=size; setPadding(0,12,0,12); setTextIsSelectable(true) }) }
        val number = DecimalFormat("0.##")
        if(courses.isEmpty()) label("Aún no estás inscrito en cursos.")
        courses.forEach { course ->
            label(course.name,22f)
            if(course.rows.isEmpty()) label("Este curso aún no tiene tareas.")
            else {
                label("Acumulado: ${number.format(course.earned)} / ${course.available} puntos publicados\n${course.gradedCount} de ${course.rows.size} tareas calificadas")
                label(if(course.gradedCount == 0) "Aún no hay calificaciones. Las tareas pendientes no tienen nota asignada."
                    else "Sobre tareas calificadas: ${number.format(course.earned)} / ${course.gradedMaximum}. El total publicado incluye tareas pendientes.",14f)
            }
            course.rows.forEach { row ->
                label(row.title,18f)
                label(when {
                    row.grade != null -> "Nota: ${number.format(row.grade)} / ${row.maximum}\nComentario del docente: ${row.feedback}"
                    row.submitted -> "Entregada · Pendiente de calificación"
                    else -> "Sin entregar · Sin calificación"
                })
                form.addView(Button(this).apply { text="Ver tarea"; isAllCaps=false
                    setOnClickListener { startActivity(Intent(this@GradebookActivity,TaskDetailActivity::class.java).putExtra("task_id",row.taskId)) }
                })
            }
        }
    }
}
