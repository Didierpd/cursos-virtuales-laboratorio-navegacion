package com.cursosvirtuales.app.cursos

import android.content.Context
import com.cursosvirtuales.app.data.AuthRepository
import com.cursosvirtuales.app.data.SessionManager
import com.cursosvirtuales.app.model.Role

object CourseAccess {
    @JvmStatic fun user(context: Context): com.cursosvirtuales.app.model.User? {
        AuthRepository.init(context)
        val session = SessionManager(context)
        if (!session.isSessionValid()) return null
        return session.currentUserId()?.let { AuthRepository.findById(it) }
    }
    @JvmStatic fun allow(context: Context, courseId: Long, write: Boolean): Boolean {
        val user = user(context) ?: return false
        val course = CourseRepository(context).buscarPorId(courseId) ?: return false
        return if (user.role == Role.DOCENTE) course.docenteId == user.id
        else !write && EnrollmentRepository(context).estaInscrito(courseId, user.id)
    }
}
