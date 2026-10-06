package com.cursosvirtuales.app.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.cursosvirtuales.app.data.AuthRepository
import com.cursosvirtuales.app.data.SessionManager
import com.cursosvirtuales.app.databinding.ActivityProfileBinding
import com.cursosvirtuales.app.model.User

/**
 * HU-26 — Consultar y editar perfil.
 * Cubre: pantalla consulta/edición (2.1), carga de foto (2.5),
 * cambio de contraseña (2.6) y control de acceso: solo el dueño edita
 * su propio perfil (2.7) — se aplica leyendo siempre al usuario de la
 * sesión activa, nunca un id externo.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager
    private var currentUser: User? = null
    private var isEditing = false

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                binding.ivProfilePhoto.setImageURI(it)
                selectedPhotoUri = it.toString()
            }
        }
    private var selectedPhotoUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthRepository.init(this)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
    }

    override fun onResume() {
        super.onResume()

        // Control de acceso (tarea 2.7): sin sesión válida, no hay perfil que mostrar.
        if (!sessionManager.isSessionValid()) {
            finish()
            return
        }
        sessionManager.touch()

        // Siempre se carga el perfil del usuario dueño de la sesión (nunca uno externo).
        val userId = sessionManager.currentUserId() ?: return finish()
        currentUser = AuthRepository.findById(userId) ?: return finish()

        bindProfile(currentUser!!)
        setEditingMode(false)

        binding.btnEditProfile.setOnClickListener { setEditingMode(true) }
        binding.btnCancelEdit.setOnClickListener {
            bindProfile(currentUser!!) // descarta cambios no guardados
            setEditingMode(false)
        }
        binding.btnSaveProfile.setOnClickListener { saveProfile() }
        binding.btnChangePhoto.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnUpdatePassword.setOnClickListener { updatePassword() }
    }

    private fun bindProfile(user: User) {
        binding.etFullName.setText(user.fullName)
        binding.etEmail.setText(user.email)
        user.photoUri?.let { binding.ivProfilePhoto.setImageURI(Uri.parse(it)) }
    }

    private fun setEditingMode(editing: Boolean) {
        isEditing = editing
        binding.etFullName.isEnabled = editing
        binding.etEmail.isEnabled = editing
        binding.btnEditProfile.visibility = if (editing) android.view.View.GONE else android.view.View.VISIBLE
        binding.btnSaveProfile.visibility = if (editing) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnCancelEdit.visibility = if (editing) android.view.View.VISIBLE else android.view.View.GONE
    }

    /** Actualización con validación de campos obligatorios (tarea 2.4). */
    private fun saveProfile() {
        val user = currentUser ?: return
        val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()

        if (fullName.isBlank() || email.isBlank()) {
            showMessage(
                getString(com.cursosvirtuales.app.R.string.profile_error_required),
                isError = true
            )
            return
        }

        val ok = AuthRepository.updateProfile(user.id, fullName, email, selectedPhotoUri)
        if (ok) {
            showMessage(getString(com.cursosvirtuales.app.R.string.profile_saved_ok), isError = false)
            setEditingMode(false)
        } else {
            showMessage(
                getString(com.cursosvirtuales.app.R.string.profile_error_required),
                isError = true
            )
        }
    }

    private fun updatePassword() {
        val user = currentUser ?: return
        val current = binding.etCurrentPassword.text?.toString().orEmpty()
        val new = binding.etNewPassword.text?.toString().orEmpty()

        when (AuthRepository.updatePassword(user.id, current, new)) {
            is AuthRepository.PasswordChangeResult.Success -> {
                binding.etCurrentPassword.text?.clear()
                binding.etNewPassword.text?.clear()
                showMessage(getString(com.cursosvirtuales.app.R.string.profile_password_ok), isError = false)
            }
            is AuthRepository.PasswordChangeResult.WrongCurrentPassword -> {
                showMessage(getString(com.cursosvirtuales.app.R.string.profile_password_wrong), isError = true)
            }
            is AuthRepository.PasswordChangeResult.EmptyFields -> {
                showMessage(getString(com.cursosvirtuales.app.R.string.profile_error_required), isError = true)
            }
        }
    }

    private fun showMessage(text: String, isError: Boolean) {
        binding.tvProfileMessage.text = text
        binding.tvProfileMessage.setTextColor(
            if (isError) getColor(com.cursosvirtuales.app.R.color.error)
            else getColor(com.cursosvirtuales.app.R.color.brand_primary)
        )
        binding.tvProfileMessage.visibility = android.view.View.VISIBLE
    }
}
