package com.cursosvirtuales.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cursosvirtuales.app.data.AuthRepository
import com.cursosvirtuales.app.data.SessionManager
import com.cursosvirtuales.app.databinding.ActivityLoginBinding
import com.cursosvirtuales.app.model.Role

/**
 * HU-25 — Iniciar y cerrar sesión.
 * Cubre: pantalla y estados de error (1.1), redirección según rol (1.6),
 * mensajes de error por credenciales inválidas (1.7).
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthRepository.init(this)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Si ya hay una sesión válida, saltar directo al panel según rol.
        if (sessionManager.isSessionValid()) {
            goToMain()
            return
        }

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        hideError()
        val username = binding.etUsername.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        setLoading(true)

        when (val result = AuthRepository.login(username, password)) {
            is AuthRepository.LoginResult.EmptyFields -> {
                setLoading(false)
                showError(getString(com.cursosvirtuales.app.R.string.login_error_empty))
            }
            is AuthRepository.LoginResult.InvalidCredentials -> {
                setLoading(false)
                showError(getString(com.cursosvirtuales.app.R.string.login_error_invalid))
            }
            is AuthRepository.LoginResult.Success -> {
                sessionManager.startSession(result.user.id)
                setLoading(false)
                goToMain()
            }
        }
    }

    /** Redirección al inicio según rol (tarea 1.6). */
    private fun goToMain() {
        val userId = sessionManager.currentUserId() ?: return
        val user = AuthRepository.findById(userId) ?: return
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_USER_ID, user.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        binding.tvLoginError.text = message
        binding.tvLoginError.visibility = android.view.View.VISIBLE
    }

    private fun hideError() {
        binding.tvLoginError.visibility = android.view.View.GONE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressLogin.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnLogin.isEnabled = !loading
    }
}
