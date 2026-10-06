package com.cursosvirtuales.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cursosvirtuales.app.data.AuthRepository
import com.cursosvirtuales.app.data.SessionManager
import com.cursosvirtuales.app.databinding.ActivityRegisterBinding
import com.cursosvirtuales.app.model.Role

/**
 * Registro de una cuenta nueva. El usuario elige su rol (Docente o
 * Estudiante) libremente. Al registrarse exitosamente, inicia sesión
 * automáticamente y va directo al panel principal.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthRepository.init(this)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.tvGoLogin.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        hideError()

        val username = binding.etUsername.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val role = if (binding.rbDocente.isChecked) Role.DOCENTE else Role.ESTUDIANTE

        when (val result = AuthRepository.register(username, password, role, fullName, email)) {
            is AuthRepository.RegisterResult.EmptyFields -> {
                showError(getString(com.cursosvirtuales.app.R.string.register_error_empty))
            }
            is AuthRepository.RegisterResult.UsernameTaken -> {
                showError(getString(com.cursosvirtuales.app.R.string.register_error_username_taken))
            }
            is AuthRepository.RegisterResult.Success -> {
                sessionManager.startSession(result.user.id)
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_USER_ID, result.user.id)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun showError(message: String) {
        binding.tvRegisterError.text = message
        binding.tvRegisterError.visibility = android.view.View.VISIBLE
    }

    private fun hideError() {
        binding.tvRegisterError.visibility = android.view.View.GONE
    }
}
