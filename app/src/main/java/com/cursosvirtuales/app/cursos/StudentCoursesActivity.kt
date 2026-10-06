package com.cursosvirtuales.app.cursos

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.cursosvirtuales.app.model.Role

class StudentCoursesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); title = "Mis cursos y tareas" }
    override fun onResume() {
        super.onResume()
        val user = CourseAccess.user(this)
        if (user?.role != Role.ESTUDIANTE) { finish(); return }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,24,24,24) }
        val scroll = ScrollView(this).apply { addView(layout) }; setContentView(scroll)
        layout.addView(Button(this).apply {
            text = "Mis calificaciones y retroalimentación"
            setOnClickListener { startActivity(Intent(this@StudentCoursesActivity,GradebookActivity::class.java)) }
        })
        val courses = CourseRepository(this).listarTodos().filter { EnrollmentRepository(this).estaInscrito(it.id,user.id) }
        if (courses.isEmpty()) layout.addView(TextView(this).apply { text = "Aún no estás inscrito en cursos. Entra a Cursos disponibles desde el inicio." })
        courses.forEach { course ->
            layout.addView(Button(this).apply {
                text = course.nombre + " · Ver unidades y tareas"
                setOnClickListener { startActivity(Intent(this@StudentCoursesActivity,CourseDetailActivity::class.java).putExtra(CourseDetailActivity.EXTRA_CURSO_ID,course.id)) }
            })
            TaskRepository(this).listarPorCurso(course.id).forEach { task ->
                layout.addView(Button(this).apply {
                    isAllCaps = false
                    text = task.toString() + "\n" + LearningRepository.status(this@StudentCoursesActivity,task)
                    setOnClickListener { startActivity(Intent(this@StudentCoursesActivity,TaskDetailActivity::class.java).putExtra("task_id",task.id)) }
                })
            }
        }
    }
}
