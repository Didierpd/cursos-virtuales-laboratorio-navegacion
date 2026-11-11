package com.cursosvirtuales.app.servicios

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.cursosvirtuales.app.R
import com.cursosvirtuales.app.data.SessionManager

object AvisosAndroid {
    // La sesión y la publicación se coordinan para cancelar avisos al cambiar de cuenta.
    val bloqueoSesion = Any()
    private const val CANAL = "actividad_academica"

    fun habilitados(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 24 && !manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(CANAL)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        return true
    }

    fun procesar(context: Context, demostracion: Long = -1) {
        val session = SessionManager(context)
        if (!session.isSessionValid()) return
        val usuario = session.currentUserId() ?: return
        val intent = Intent(context, DelayedMessageService::class.java)
            .putExtra("destinatario", usuario).putExtra("demostracion", demostracion)
        try {
            context.startService(intent)
        } catch (_: IllegalStateException) {
            Log.i("StartedServices", "Android pospuso el servicio. Los avisos siguen pendientes.")
        }
    }

    fun publicar(context: Context, aviso: AvisosRepository.Aviso): Boolean = synchronized(bloqueoSesion) {
        val session = SessionManager(context)
        if (!session.isSessionValid() || session.currentUserId() != aviso.destinatario || !habilitados(context)) {
            return@synchronized false
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(CANAL, "Actividad académica", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val intent = Intent(context, AvisosActivity::class.java)
            .setData(Uri.parse("cursosvirtuales://aviso/" + aviso.id))
            .putExtra("destinatario", aviso.destinatario).putExtra("aviso_id", aviso.id)
        val pendiente = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val publico = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_aviso).setContentTitle("Cursos Virtuales")
            .setContentText("Tienes un aviso académico.").build()
        val notificacion = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_aviso).setContentTitle(aviso.titulo).setContentText(aviso.mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(aviso.mensaje))
            .setContentIntent(pendiente).setAutoCancel(true).setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(publico).build()
        try {
            // El tag identifica el evento y conserva avisos distintos sin reemplazarlos.
            manager.notify("aviso_" + aviso.id, 1, notificacion)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun limpiar(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancelAll()
    }
}
