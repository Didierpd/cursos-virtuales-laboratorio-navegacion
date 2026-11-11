package com.cursosvirtuales.app.servicios

import android.app.IntentService
import android.content.Intent
import android.util.Log

/** Ejercicio de la guía: IntentService procesa solicitudes en su hilo de trabajo. */
@Suppress("DEPRECATION")
class DelayedMessageService : IntentService("DelayedMessageService") {
    override fun onCreate() {
        super.onCreate()
        Log.i("StartedServices", "onCreate")
    }

    override fun onHandleIntent(intent: Intent?) {
        val usuario = intent?.getStringExtra("destinatario") ?: return
        val demoId = intent.getLongExtra("demostracion", -1)
        Log.i("StartedServices", "onHandleIntent en " + Thread.currentThread().name)
        if (demoId > 0) {
            try { Thread.sleep(10_000) }
            catch (_: InterruptedException) { Thread.currentThread().interrupt(); return }
        }
        val repo = AvisosRepository(this)
        val pendientes = repo.listar(usuario).filter { !it.publicado && it.disponibleDesde <= System.currentTimeMillis() }
        for (aviso in pendientes) {
            if (demoId > 0 && aviso.id != demoId) continue
            if (demoId > 0) Log.i("StartedServices", "Recordatorio de demostración: " + aviso.mensaje)
            if (AvisosAndroid.publicar(this, aviso)) {
                repo.publicado(aviso.id, usuario)
                Log.i("StartedServices", "Notificación publicada, evento " + aviso.id)
            }
        }
    }

    override fun onDestroy() {
        Log.i("StartedServices", "onDestroy")
        super.onDestroy()
    }
}
