package com.cursosvirtuales.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cursosvirtuales.app.cursos.AvailableCoursesActivity
import com.cursosvirtuales.app.cursos.CourseListActivity
import com.cursosvirtuales.app.data.AuthRepository
import com.cursosvirtuales.app.data.SessionManager
import com.cursosvirtuales.app.databinding.ActivityMainBinding
import com.cursosvirtuales.app.model.Role

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthRepository.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
    }

    override fun onResume() {
        super.onResume()

        // Control de sesión: si expiró por inactividad, regresar a Login (tarea 1.4).
        if (!sessionManager.isSessionValid()) {
            goToLogin(expired = true)
            return
        }
        sessionManager.touch()

        val userId = sessionManager.currentUserId() ?: return goToLogin(expired = false)
        val user = AuthRepository.findById(userId) ?: return goToLogin(expired = false)

        binding.tvWelcome.text = getString(com.cursosvirtuales.app.R.string.home_welcome, user.fullName)
        binding.tvRolePanel.text = when (user.role) {
            Role.DOCENTE -> getString(com.cursosvirtuales.app.R.string.home_role_docente)
            Role.ESTUDIANTE -> getString(com.cursosvirtuales.app.R.string.home_role_estudiante)
        }

        // HU-03: solo el docente ve el acceso a "Mis cursos".
        if (user.role == Role.DOCENTE) {
            binding.btnMisCursos.visibility = android.view.View.VISIBLE
            binding.btnMisCursos.setOnClickListener {
                startActivity(Intent(this, CourseListActivity::class.java))
            }
        } else {
            binding.btnMisCursos.visibility = android.view.View.VISIBLE
            binding.btnMisCursos.text = "Mis cursos y tareas"
            binding.btnMisCursos.setOnClickListener {
                startActivity(Intent(this, com.cursosvirtuales.app.cursos.StudentCoursesActivity::class.java))
            }
        }

        // HU-04: solo el estudiante ve el acceso a "Cursos disponibles".
        if (user.role == Role.ESTUDIANTE) {
            binding.btnCursosDisponibles.visibility = android.view.View.VISIBLE
            binding.btnCursosDisponibles.setOnClickListener {
                startActivity(Intent(this, AvailableCoursesActivity::class.java))
            }
        } else {
            binding.btnCursosDisponibles.visibility = android.view.View.GONE
        }

        binding.btnCalificaciones.visibility = if (user.role == Role.ESTUDIANTE) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnCalificaciones.setOnClickListener {
            startActivity(Intent(this, com.cursosvirtuales.app.cursos.GradebookActivity::class.java))
        }
        binding.btnServiciosRest.setOnClickListener {
            startActivity(Intent(this, RestServicesActivity::class.java))
        }
        binding.btnGoProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.logout()
            goToLogin(expired = false)
        }
    }

    private fun goToLogin(expired: Boolean) {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
