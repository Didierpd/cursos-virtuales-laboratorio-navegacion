package com.cursosvirtuales.app.servicios

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.cursosvirtuales.app.cursos.TaskDetailActivity
import com.cursosvirtuales.app.data.SessionManager
import com.cursosvirtuales.app.talleres.TallerActivity

class AvisosActivity : TallerActivity() {
    private lateinit var contenido: LinearLayout
    private val permiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        AvisosAndroid.procesar(this)
        mostrar()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Avisos y servicios"
    }

    override fun onResume() {
        super.onResume()
        val session = SessionManager(this)
        if (!session.isSessionValid()) return
        val destinatario = intent.getStringExtra("destinatario")
        if (destinatario != null && destinatario != session.currentUserId()) {
            Toast.makeText(this, "Este aviso corresponde a otra cuenta. Ingresa con la cuenta destinataria.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        AvisosAndroid.procesar(this)
        mostrar()
    }

    private fun texto(mensaje: String) {
        contenido.addView(TextView(this).apply { text = mensaje; textSize = 16f; setPadding(0, 12, 0, 12) })
    }

    private fun boton(titulo: String, accion: () -> Unit) {
        contenido.addView(Button(this).apply { text = titulo; isAllCaps = false; setOnClickListener { accion() } })
    }

    private fun mostrar() {
        val session = SessionManager(this)
        if (!session.isSessionValid() || isFinishing) return
        val usuario = session.currentUserId() ?: return
        contenido = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 32) }
        setContentView(ScrollView(this).apply { addView(contenido) })
        texto(if (AvisosAndroid.habilitados(this)) "Notificaciones activadas" else "Activa las notificaciones para recibir avisos en la barra.")
        boton("Activar notificaciones") {
            if (Build.VERSION.SDK_INT >= 33 && !AvisosAndroid.habilitados(this)) {
                permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            }
        }
        val mensaje = EditText(this).apply {
            hint = "Mensaje del recordatorio"
            setText("Recuerda revisar tus tareas en Cursos Virtuales.")
        }
        contenido.addView(mensaje)
        boton("Notificar después de 10 segundos") {
            val texto = mensaje.text.toString().trim()
            if (texto.isEmpty() || texto.length > 300) {
                mensaje.error = "Escribe un mensaje de 1 a 300 caracteres"
            } else {
                val id = AvisosRepository(this).demostracion(usuario, texto)
                AvisosAndroid.procesar(this, id)
                Toast.makeText(this, "Recordatorio en cola. Puedes salir de la pantalla mientras se procesa.", Toast.LENGTH_LONG).show()
            }
        }
        boton("Actualizar avisos") { AvisosAndroid.procesar(this); mostrar() }
        texto("Tus avisos")
        val avisos = AvisosRepository(this).listar(usuario)
        if (avisos.isEmpty()) texto("Todavía no tienes avisos.")
        for (aviso in avisos) {
            texto(aviso.titulo + "\n" + aviso.mensaje)
            if (aviso.tareaId != null) boton("Abrir tarea") {
                startActivity(Intent(this, TaskDetailActivity::class.java).putExtra("task_id", aviso.tareaId))
            }
        }
    }
}
