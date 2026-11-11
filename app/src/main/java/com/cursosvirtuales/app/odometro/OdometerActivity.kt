package com.cursosvirtuales.app.odometro

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.text.InputType
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.cursosvirtuales.app.data.SessionManager
import com.cursosvirtuales.app.talleres.TallerActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OdometerActivity : TallerActivity() {
    private var servicio: OdometerService? = null
    private var solicitado = false
    private var visible = false
    private val repo by lazy { OdometerRepository(this) }
    private lateinit var usuario: String
    private lateinit var formacion: EditText
    private lateinit var precision: EditText
    private lateinit var intervalo: EditText
    private lateinit var estado: TextView
    private lateinit var metros: TextView
    private lateinit var posicion: TextView
    private lateinit var iniciar: Button
    private lateinit var pausar: Button
    private lateinit var finalizar: Button
    private lateinit var reporte: LinearLayout
    private var registroMostrado: Long? = null
    private var accionPendiente: (() -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private val permiso = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val accion = accionPendiente
        accionPendiente = null
        if (conPermiso()) accion?.invoke()
        else mensaje("Concede ubicación para registrar metros. El historial sigue disponible.")
    }
    private val conexion = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            servicio = (binder as OdometerService.OdometerBinder).getOdometer()
            servicio?.cargar()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            servicio = null
        }
    }
    private val actualizar = object : Runnable {
        override fun run() {
            if (!visible) return
            val session = SessionManager(this@OdometerActivity)
            if (!session.isSessionValid() || session.currentUserId() != usuario) { finish(); return }
            val actual = servicio?.estado?.takeIf { it.usuario == usuario }
            estado.text = actual?.mensaje ?: "Conectando con el servicio…"
            metros.text = String.format(Locale.getDefault(), "Distancia recorrida: %.2f m", actual?.registro?.metros ?: 0.0)
            posicion.text = actual?.ubicacion?.let {
                "Ubicación actual: ${it.latitude}, ${it.longitude}\nPrecisión GPS aproximada: ${it.accuracy.toInt()} m"
            } ?: actual?.registro?.let { registro ->
                if (registro.latitud != null) "Última ubicación guardada: ${registro.latitud}, ${registro.longitud}"
                else "Ubicación actual pendiente de recibir del dispositivo."
            } ?: "Ubicación actual pendiente de recibir del dispositivo."
            val registro = actual?.registro
            if (registro != null && registroMostrado != registro.id) {
                formacion.setText(registro.formacion)
                precision.setText(registro.precision.toString())
                intervalo.setText(registro.intervalo.toString())
                registroMostrado = registro.id
            }
            if (actual != null && registro == null) registroMostrado = null
            formacion.isEnabled = registro == null
            precision.isEnabled = registro == null
            intervalo.isEnabled = registro == null
            iniciar.isEnabled = actual != null && registro?.estado != "ACTIVO"
            pausar.isEnabled = registro?.estado == "ACTIVO"
            finalizar.isEnabled = registro != null
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { usuario = repo.estudiante() }
        catch (_: IllegalArgumentException) { mensaje("El odómetro está disponible para estudiantes."); finish(); return }
        title = "Odómetro de tutoría"
        val layout = columna()
        setContentView(ScrollView(this).apply { addView(layout) })
        texto(layout, "Registra el desplazamiento hacia tu sesión presencial de tutoría.")
        formacion = campo(layout, "Proceso formativo o tutoría", InputType.TYPE_CLASS_TEXT)
        formacion.setText(savedInstanceState?.getString("formacion") ?: intent.getStringExtra("formacion").orEmpty())
        texto(layout, "Precisión del movimiento (m): distancia mínima para sumar un tramo.")
        precision = campo(layout, "Precisión del movimiento (metros)", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        precision.setText(savedInstanceState?.getString("precision") ?: "5")
        texto(layout, "Tiempo de actualización (segundos)")
        intervalo = campo(layout, "Intervalo en segundos", InputType.TYPE_CLASS_NUMBER)
        intervalo.setText(savedInstanceState?.getString("intervalo") ?: "5")
        posicion = texto(layout, "Ubicación actual pendiente de recibir del dispositivo.")
        boton(layout, "Obtener ubicación actual") { pedirUbicacion { servicio?.obtenerUbicacion() } }
        boton(layout, "Ajustes de ubicación") { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
        estado = texto(layout, "Conectando con el servicio…")
        metros = texto(layout, "Distancia recorrida: 0.00 m")
        iniciar = boton(layout, "Iniciar / Reanudar recorrido") {
            val nombre = formacion.text.toString().trim()
            val distancia = precision.text.toString().replace(',', '.').toFloatOrNull()
            val segundos = intervalo.text.toString().toLongOrNull()
            when {
                nombre.isEmpty() -> formacion.error = "Indica el proceso formativo"
                distancia == null || !distancia.isFinite() || distancia <= 0 -> precision.error = "Escribe un número positivo"
                segundos == null || segundos <= 0 || segundos > Long.MAX_VALUE / 1000 -> intervalo.error = "Escribe segundos enteros positivos"
                else -> pedirUbicacion { servicio?.iniciar(nombre, distancia, segundos) }
            }
        }
        pausar = boton(layout, "Pausar recorrido") { servicio?.pausar() }
        finalizar = boton(layout, "Finalizar y guardar recorrido") { servicio?.finalizar() }
        texto(layout, "Al salir de esta pantalla se guarda y pausa el recorrido. Al volver, pulsa Reanudar para continuar. Finaliza para registrar otra tutoría o cambiar la configuración.")
        boton(layout, "Reporte consolidado") { mostrarReporte() }
        reporte = columna()
        layout.addView(reporte)
    }

    override fun onStart() {
        super.onStart()
        if (isFinishing) return
        visible = true
        solicitado = bindService(Intent(this, OdometerService::class.java), conexion, Context.BIND_AUTO_CREATE)
        handler.post(actualizar)
    }

    override fun onStop() {
        visible = false
        handler.removeCallbacks(actualizar)
        servicio?.pausar()
        if (solicitado) unbindService(conexion)
        solicitado = false
        servicio = null
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::formacion.isInitialized) {
            outState.putString("formacion", formacion.text.toString())
            outState.putString("precision", precision.text.toString())
            outState.putString("intervalo", intervalo.text.toString())
        }
        super.onSaveInstanceState(outState)
    }

    private fun conPermiso() = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun pedirUbicacion(accion: () -> Unit) {
        if (servicio == null) { mensaje("Espera a que el servicio se conecte."); return }
        if (conPermiso()) accion()
        else {
            accionPendiente = accion
            permiso.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun mostrarReporte() {
        val session = SessionManager(this)
        if (!session.isSessionValid() || session.currentUserId() != usuario) { finish(); return }
        reporte.removeAllViews()
        val resumen = repo.resumen()
        texto(reporte, String.format(Locale.getDefault(), "Total acumulado: %.2f m", resumen.sumOf { it.metros }))
        if (resumen.isEmpty()) texto(reporte, "Todavía no tienes recorridos guardados.")
        resumen.forEach { texto(reporte, String.format(Locale.getDefault(), "%s · %d recorrido(s) · %.2f m", it.formacion, it.recorridos, it.metros)) }
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        repo.historial().forEach { registro ->
            val simulacion = if (registro.simulado) " · Incluye ubicación simulada" else ""
            texto(reporte, "${registro.formacion} · ${formato.format(Date(registro.inicio))}\n" +
                String.format(Locale.getDefault(), "%.2f m · %s · %d muestras", registro.metros, registro.estado, registro.muestras) +
                "\nMovimiento mínimo: ${registro.precision} m · Intervalo: ${registro.intervalo} s$simulacion")
        }
    }

    private fun columna() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 12, 24, 12) }
    private fun texto(layout: LinearLayout, valor: String) = TextView(this).apply { text = valor; textSize = 16f; setPadding(0, 8, 0, 8); layout.addView(this) }
    private fun campo(layout: LinearLayout, titulo: String, tipo: Int) = EditText(this).apply { hint = titulo; inputType = tipo; setSingleLine(); layout.addView(this) }
    private fun boton(layout: LinearLayout, titulo: String, accion: () -> Unit) = Button(this).apply { text = titulo; isAllCaps = false; setOnClickListener { accion() }; layout.addView(this) }
    private fun mensaje(valor: String) = Toast.makeText(this, valor, Toast.LENGTH_LONG).show()
}
