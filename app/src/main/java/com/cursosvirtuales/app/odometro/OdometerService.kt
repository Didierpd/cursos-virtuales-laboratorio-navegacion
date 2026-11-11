package com.cursosvirtuales.app.odometro

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.cursosvirtuales.app.data.SessionManager
import java.util.UUID
import kotlin.math.max

/** Servicio exclusivamente enlazado. El hilo de GPS serializa cálculo y persistencia. */
class OdometerService : Service(), LocationListener {
    inner class OdometerBinder : Binder() { fun getOdometer() = this@OdometerService }
    data class Estado(val registro: OdometerRepository.Registro? = null, val ubicacion: Location? = null,
        val mensaje: String = "Servicio conectado. Obtén tu ubicación o inicia un recorrido.", val usuario: String? = null)

    @Volatile var estado = Estado()
        private set
    private lateinit var hilo: HandlerThread
    private lateinit var trabajo: Handler
    private val repo by lazy { OdometerRepository(this) }
    private val ubicacion by lazy { getSystemService(LocationManager::class.java) }
    private var estudiante: String? = null
    private var segmento: String? = null
    private var ancla: Location? = null
    private var ultimoTiempo = 0L
    private var inicioSegmento = 0L
    private var puntual: CancellationSignal? = null

    override fun onCreate() {
        super.onCreate()
        hilo = HandlerThread("OdometerGps").apply { start() }
        trabajo = Handler(hilo.looper)
        Log.i("BoundOdometer", "onCreate")
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i("BoundOdometer", "onBind")
        return OdometerBinder()
    }

    fun getDistance(): Double = estado.registro?.metros ?: 0.0

    fun cargar() {
        val usuario = repo.estudiante()
        ejecutar {
            if (estudiante != usuario) {
                pausarInterno()
                estudiante = usuario
                estado = Estado(repo.recuperar(usuario), usuario = usuario)
            } else if (segmento == null) {
                estado = estado.copy(registro = repo.recuperar(usuario))
            }
        }
    }

    fun iniciar(formacion: String, precision: Float, intervalo: Long) {
        val usuario = repo.estudiante()
        ejecutar {
            if (!sesionValida(usuario)) return@ejecutar
            if (segmento != null) return@ejecutar
            val proveedor = proveedor() ?: return@ejecutar
            estudiante = usuario
            val token = UUID.randomUUID().toString()
            val registro = repo.iniciar(usuario, formacion, precision, intervalo, token)
            segmento = token
            ancla = null
            ultimoTiempo = 0
            inicioSegmento = SystemClock.elapsedRealtimeNanos()
            estado = Estado(registro, mensaje = "Esperando una ubicación nueva de $proveedor…", usuario = usuario)
            try {
                ubicacion.requestLocationUpdates(proveedor, registro.intervalo * 1000,
                    registro.precision, this, hilo.looper)
            } catch (_: SecurityException) {
                pausarInterno()
                estado = estado.copy(mensaje = "Concede el permiso de ubicación para registrar el recorrido.")
            }
        }
    }

    fun pausar() = ejecutar { pausarInterno() }

    fun finalizar() {
        val usuario = repo.estudiante()
        ejecutar {
            pausarInterno()
            val registro = repo.listar(usuario).firstOrNull { it.estado != "FINALIZADO" }
            if (registro != null) repo.finalizar(registro.id, usuario)
            estado = Estado(mensaje = "Recorrido guardado. Puedes consultar el reporte o iniciar otra tutoría.", usuario = usuario)
        }
    }

    fun obtenerUbicacion() {
        val usuario = repo.estudiante()
        ejecutar {
            if (!sesionValida(usuario)) return@ejecutar
            val proveedor = proveedor() ?: return@ejecutar
            puntual?.cancel()
            val cancelacion = CancellationSignal()
            puntual = cancelacion
            estado = estado.copy(mensaje = "Obteniendo ubicación actual…")
            trabajo.postDelayed({
                if (puntual === cancelacion) {
                    cancelacion.cancel()
                    puntual = null
                    estado = estado.copy(mensaje = "Sin señal. Revisa que la ubicación esté activada e intenta de nuevo.")
                }
            }, 15_000)
            LocationManagerCompat.getCurrentLocation(ubicacion, proveedor, cancelacion,
                java.util.concurrent.Executor { trabajo.post(it) }) { posicion ->
                if (puntual !== cancelacion) return@getCurrentLocation
                puntual = null
                if (sesionValida(usuario)) estado = estado.copy(ubicacion = posicion?.let(::Location),
                    mensaje = if (posicion == null) "No se obtuvo ubicación. Intenta de nuevo." else "Ubicación actual recibida.")
            }
        }
    }

    private fun proveedor(): String? {
        val fino = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val aproximado = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fino && !aproximado) {
            estado = estado.copy(mensaje = "Concede el permiso de ubicación.")
            return null
        }
        val candidatos = if (fino) listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER) else listOf(LocationManager.NETWORK_PROVIDER)
        return candidatos.firstOrNull { ubicacion.isProviderEnabled(it) }.also {
            if (it == null) estado = estado.copy(mensaje = "Activa la ubicación del dispositivo para iniciar el recorrido.")
        }
    }

    override fun onLocationChanged(location: Location) {
        val usuario = estudiante ?: return
        val token = segmento ?: return
        if (!sesionValida(usuario)) { pausarInterno(); return }
        val registro = estado.registro ?: return
        if (!location.latitude.isFinite() || !location.longitude.isFinite() ||
            location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0 ||
            !location.hasAccuracy() || !location.accuracy.isFinite() || location.accuracy < 0) return
        val tiempo = location.elapsedRealtimeNanos
        if (tiempo < inicioSegmento || tiempo <= ultimoTiempo) return
        if (ultimoTiempo != 0L && (tiempo - ultimoTiempo) / 1_000_000 < registro.intervalo * 1000) return
        val anterior = ancla
        val tramo = anterior?.distanceTo(location)?.toDouble() ?: 0.0
        val umbral = max(registro.precision, max(anterior?.accuracy ?: 0f, location.accuracy))
        val sumar = if (tramo >= umbral) tramo else 0.0
        if (!repo.muestra(registro.id, usuario, token, location, sumar)) return
        if (anterior == null || sumar > 0) ancla = Location(location)
        ultimoTiempo = tiempo
        estado = Estado(repo.listar(usuario).first { it.id == registro.id }, Location(location),
            "Registrando metros. Precisión GPS aproximada: ${location.accuracy.toInt()} m", usuario)
    }

    @Deprecated("API de compatibilidad para Android anterior a 30")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) {
        pausarInterno()
        estado = estado.copy(mensaje = "Ubicación desactivada. Actívala y reanuda el recorrido.")
    }

    private fun sesionValida(usuario: String): Boolean {
        val session = SessionManager(this)
        return session.isSessionValid() && session.currentUserId() == usuario
    }

    private fun pausarInterno() {
        ubicacion.removeUpdates(this)
        puntual?.cancel()
        puntual = null
        val usuario = estudiante
        val registro = estado.registro
        val token = segmento
        if (usuario != null && registro != null && token != null) repo.pausar(registro.id, usuario, token)
        segmento = null
        ancla = null
        if (usuario != null) estado = estado.copy(registro = repo.listar(usuario).firstOrNull { it.estado != "FINALIZADO" }, mensaje = "Recorrido en pausa y guardado.")
    }

    private fun ejecutar(accion: () -> Unit) {
        trabajo.post {
            try { accion() }
            catch (e: IllegalArgumentException) { estado = estado.copy(mensaje = e.message ?: "Revisa los datos del recorrido.") }
            catch (e: SecurityException) { estado = estado.copy(mensaje = "Permiso de ubicación no disponible.") }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i("BoundOdometer", "onUnbind")
        pausar()
        return false
    }

    override fun onDestroy() {
        Log.i("BoundOdometer", "onDestroy")
        trabajo.post { try { pausarInterno() } finally { hilo.quitSafely() } }
        super.onDestroy()
    }
}
