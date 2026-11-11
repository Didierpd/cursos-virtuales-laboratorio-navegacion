package com.cursosvirtuales.app.talleres.mapas

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.cursosvirtuales.app.data.SessionManager
import com.cursosvirtuales.app.talleres.TallerActivity
import com.cursosvirtuales.app.talleres.encomiendas.EncomiendasDbHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

class RutasActivity : TallerActivity() {
    private lateinit var contenido: LinearLayout
    private lateinit var resultados: LinearLayout
    private lateinit var latitud: EditText
    private lateinit var longitud: EditText
    private lateinit var modos: Spinner
    private lateinit var recolector: EditText
    private lateinit var fecha: Button
    private lateinit var estadoGps: TextView
    private lateinit var db: EncomiendasDbHelper
    private var solicitud: CancellationSignal? = null
    private val handler = Handler(Looper.getMainLooper())
    private val permiso = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (tienePermiso()) leerGps() else aviso("Puedes escribir el origen manualmente.")
    }
    private val calendario = Calendar.getInstance()
    private val tutoria get() = intent.getBooleanExtra("tutoria", false)
    private val opciones = listOf("walking", "driving", "bicycling")
    private val origenEjemplo = Punto(4.632340, -74.065350)
    private val fechaIso get() = String.format(Locale.ROOT, "%04d-%02d-%02d",
        calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH) + 1, calendario.get(Calendar.DAY_OF_MONTH))
    private val preferencias get() = getSharedPreferences("sedes_tutoria", MODE_PRIVATE)
    private val usuario get() = SessionManager(this).currentUserId().orEmpty()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        db = EncomiendasDbHelper(this)
        supportActionBar?.title = if (tutoria) "Rutas de tutoría" else "Ruta diaria de recolección"
        contenido = columna()
        setContentView(ScrollView(this).apply { addView(contenido) })
        texto(contenido, "Origen del recorrido")
        latitud = coordenada(contenido, "Latitud del origen")
        longitud = coordenada(contenido, "Longitud del origen")
        estadoGps = texto(contenido, "Usa tu ubicación o escribe las coordenadas.")
        boton(contenido, "Usar mi ubicación GPS") { pedirGps() }
        boton(contenido, "Usar origen de ejemplo en Bogotá") {
            fijarOrigen(origenEjemplo)
            estadoGps.text = "Origen de ejemplo"
        }
        boton(contenido, "Ver origen en Google Maps") { origen()?.let { MapsIntent.ubicar(this, it) } }
        texto(contenido, "Medio de transporte")
        modos = Spinner(this).also {
            it.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
                listOf("A pie", "Automóvil", "Bicicleta"))
            contenido.addView(it)
            it.setSelection(if (tutoria) 0 else 1)
        }

        if (tutoria) {
            boton(contenido, "Agregar sede de tutoría") { editarSede() }
            boton(contenido, "Comparar sedes cercanas") { mostrarSedes() }
        } else {
            recolector = campo(contenido, "Nombre o código del recolector")
            fecha = boton(contenido, "") { elegirFecha() }
            boton(contenido, "Asignar encomienda para la fecha") { elegirEncomienda() }
            boton(contenido, "Planear recorrido") { mostrarRecolecciones() }
        }
        resultados = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        contenido.addView(resultados)
        state?.let {
            latitud.setText(it.getString("latitud"))
            longitud.setText(it.getString("longitud"))
            modos.setSelection(it.getInt("modo"))
            calendario.timeInMillis = it.getLong("fecha", calendario.timeInMillis)
            if (!tutoria) recolector.setText(it.getString("recolector"))
        }
        if (!tutoria) fecha.text = "Fecha: $fechaIso"
        if (tutoria) mostrarSedesSinOrigen()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("latitud", latitud.text.toString())
        outState.putString("longitud", longitud.text.toString())
        outState.putInt("modo", modos.selectedItemPosition)
        outState.putLong("fecha", calendario.timeInMillis)
        if (!tutoria) outState.putString("recolector", recolector.text.toString())
    }

    private fun columna() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val espacio = (16 * resources.displayMetrics.density).toInt()
        setPadding(espacio, espacio, espacio, espacio)
    }

    private fun texto(padre: LinearLayout, mensaje: String) = TextView(this).apply {
        text = mensaje
        textSize = 16f
        padre.addView(this)
    }

    private fun boton(padre: LinearLayout, titulo: String, accion: () -> Unit) = Button(this).apply {
        text = titulo
        isAllCaps = false
        setOnClickListener { accion() }
        padre.addView(this)
    }

    private fun campo(padre: LinearLayout, etiqueta: String) = EditText(this).apply {
        hint = etiqueta
        setSingleLine()
        padre.addView(this)
    }

    private fun coordenada(padre: LinearLayout, etiqueta: String) = campo(padre, etiqueta).apply {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
    }

    private fun aviso(mensaje: String) = Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()

    private fun origen(): Punto? = Punto.leer(latitud.text.toString(), longitud.text.toString())
        .also { if (it == null) aviso("Indica una latitud y una longitud válidas para el origen.") }

    private fun fijarOrigen(punto: Punto) {
        latitud.setText(punto.latitud.toString())
        longitud.setText(punto.longitud.toString())
        resultados.removeAllViews()
    }

    private fun tienePermiso() = listOf(Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun pedirGps() {
        if (tienePermiso()) leerGps()
        else permiso.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun leerGps() {
        val manager = getSystemService(LocationManager::class.java)
        val fino = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val candidatos = if (fino) listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            else listOf(LocationManager.NETWORK_PROVIDER)
        val proveedor = candidatos.firstOrNull { manager.isProviderEnabled(it) }
        if (proveedor == null) {
            estadoGps.text = "Ubicación no disponible. Activa la ubicación o escribe el origen."
            return
        }
        solicitud?.cancel()
        val cancelacion = CancellationSignal()
        solicitud = cancelacion
        estadoGps.text = "Buscando ubicación…"
        handler.postDelayed({
            if (solicitud === cancelacion) {
                cancelacion.cancel()
                solicitud = null
                estadoGps.text = "Sin señal de ubicación. Intenta de nuevo o escribe el origen."
            }
        }, 15_000)
        try {
            LocationManagerCompat.getCurrentLocation(manager, proveedor, cancelacion,
                ContextCompat.getMainExecutor(this)) { posicion ->
                if (solicitud !== cancelacion) return@getCurrentLocation
                solicitud = null
                if (posicion == null) {
                    estadoGps.text = "No se obtuvo ubicación. Puedes escribir el origen."
                } else {
                    fijarOrigen(Punto(posicion.latitude, posicion.longitude))
                    estadoGps.text = "Ubicación recibida · precisión aproximada de ${posicion.accuracy.toInt()} m"
                }
            }
        } catch (_: SecurityException) {
            solicitud = null
            estadoGps.text = "Permiso de ubicación no disponible. Escribe el origen."
        }
    }

    private fun elegirFecha() {
        DatePickerDialog(this, { _, anio, mes, dia ->
            calendario.set(anio, mes, dia)
            fecha.text = "Fecha: $fechaIso"
            resultados.removeAllViews()
        }, calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun nombreRecolector(): String? = recolector.text.toString().trim().takeIf { it.isNotEmpty() }
        .also { if (it == null) recolector.error = "Indica el recolector" }

    private fun elegirEncomienda() {
        val nombre = nombreRecolector() ?: return
        val dia = fechaIso
        val encomiendas = db.consultarEncomiendas()
        if (encomiendas.isEmpty()) {
            aviso("Primero registra una encomienda.")
            return
        }
        AlertDialog.Builder(this).setTitle("Selecciona la encomienda")
            .setItems(encomiendas.map { "${it.guia} · ${it.remitente.direccion}, ${it.remitente.ciudad}" }.toTypedArray()) { _, indice ->
                val encomienda = encomiendas[indice]
                val formulario = columna()
                texto(formulario, "Recogida: ${encomienda.remitente.direccion}, ${encomienda.remitente.ciudad}")
                val lat = coordenada(formulario, "Latitud de recogida")
                val lon = coordenada(formulario, "Longitud de recogida")
                val dialogo = AlertDialog.Builder(this).setTitle(encomienda.guia).setView(formulario)
                    .setNegativeButton("Cancelar", null)
                    .setNeutralButton("Quitar asignación") { _, _ ->
                        db.quitarRecoleccion(encomienda.id, dia)
                        resultados.removeAllViews()
                        aviso("Asignación retirada para $dia.")
                    }.setPositiveButton("Guardar", null).create()
                dialogo.setOnShowListener {
                    dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val punto = Punto.leer(lat.text.toString(), lon.text.toString())
                        if (punto == null) {
                            aviso("Indica coordenadas válidas para la recogida.")
                        } else {
                            db.asignarRecoleccion(encomienda.id, dia, nombre, punto.latitud, punto.longitud)
                            resultados.removeAllViews()
                            dialogo.dismiss()
                            aviso("Encomienda asignada a $nombre para $dia.")
                        }
                    }
                }
                dialogo.show()
            }.show()
    }

    private fun mostrarRecolecciones() {
        val nombre = nombreRecolector() ?: return
        val inicio = origen() ?: return
        val paradas = db.paradasDelDia(fechaIso, nombre).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(Parada(cursor.getString(0),
                    Punto(cursor.getDouble(1), cursor.getDouble(2)),
                    "${cursor.getString(3)}, ${cursor.getString(4)}"))
            }
        }
        resultados.removeAllViews()
        if (paradas.isEmpty()) {
            texto(resultados, "No hay encomiendas asignadas a $nombre el $fechaIso.")
            return
        }
        val orden = Recorrido.ordenar(inicio, paradas)
        texto(resultados, "$fechaIso · $nombre · ${paradas.size} recogidas")
        mostrarPlan("Orden recomendado", inicio, orden, true)
        mostrarPlan("Orden de registro", inicio, paradas, false)
    }

    private fun mostrarPlan(titulo: String, inicio: Punto, paradas: List<Parada>, listar: Boolean) {
        texto(resultados, "$titulo · ${km(Recorrido.distancia(inicio, paradas))} km estimados")
        if (listar) paradas.forEachIndexed { i, parada ->
            boton(resultados, "${i + 1}. ${parada.nombre} · ${parada.detalle}") {
                MapsIntent.ubicar(this, parada.punto)
            }
        }
        Recorrido.tramos(inicio, paradas).forEachIndexed { i, (desde, grupo) ->
            boton(resultados, "Abrir $titulo · tramo ${i + 1} en Maps") {
                MapsIntent.ruta(this, desde, grupo, opciones[modos.selectedItemPosition])
            }
        }
    }

    private fun km(valor: Double) = String.format(Locale.getDefault(), "%.2f", valor)

    private fun sedesGuardadas() = JSONArray(preferencias.getString(usuario, "[]"))

    private fun sedes(): List<Parada> {
        val guardadas = sedesGuardadas()
        return buildList {
            for (i in 0 until guardadas.length()) {
                val sede = guardadas.getJSONObject(i)
                add(Parada(sede.getString("nombre"), Punto(sede.getDouble("lat"), sede.getDouble("lon"))))
            }
            if (isEmpty()) {
                add(Parada("Ejemplo: sede universitaria", origenEjemplo))
                add(Parada("Ejemplo: Parque Nacional", Punto(4.6225, -74.0636)))
                add(Parada("Ejemplo: sector Chapinero", Punto(4.6496, -74.0628)))
            }
        }
    }

    private fun editarSede() {
        val formulario = columna()
        val nombre = campo(formulario, "Docente, materia o sede")
        val lat = coordenada(formulario, "Latitud de la sede")
        val lon = coordenada(formulario, "Longitud de la sede")
        val dialogo = AlertDialog.Builder(this).setTitle("Agregar sede de tutoría").setView(formulario)
            .setNegativeButton("Cancelar", null).setPositiveButton("Guardar", null).create()
        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val punto = Punto.leer(lat.text.toString(), lon.text.toString())
                val etiqueta = nombre.text.toString().trim()
                if (punto == null || etiqueta.isEmpty()) {
                    aviso("Indica un nombre y coordenadas válidas.")
                } else {
                    val guardadas = sedesGuardadas()
                    guardadas.put(JSONObject().put("nombre", etiqueta).put("lat", punto.latitud).put("lon", punto.longitud))
                    preferencias.edit().putString(usuario, guardadas.toString()).apply()
                    dialogo.dismiss()
                    mostrarSedesSinOrigen()
                }
            }
        }
        dialogo.show()
    }

    private fun mostrarSedesSinOrigen() {
        resultados.removeAllViews()
        sedes().forEach { sede ->
            boton(resultados, "Ubicar ${sede.nombre} en Maps") { MapsIntent.ubicar(this, sede.punto) }
        }
    }

    private fun mostrarSedes() {
        val inicio = origen() ?: return
        resultados.removeAllViews()
        texto(resultados, "Sedes por distancia estimada en línea recta")
        sedes().sortedBy { inicio.kilometrosHasta(it.punto) }.forEachIndexed { i, sede ->
            val destacado = if (i == 0) "Más cercana · " else ""
            texto(resultados, "$destacado${sede.nombre} · ${km(inicio.kilometrosHasta(sede.punto))} km")
            boton(resultados, "Ubicar ${sede.nombre} en Maps") { MapsIntent.ubicar(this, sede.punto) }
            boton(resultados, "Ver ruta a ${sede.nombre} en Maps") {
                MapsIntent.ruta(this, inicio, listOf(sede), opciones[modos.selectedItemPosition])
            }
        }
    }

    override fun onDestroy() {
        solicitud?.cancel()
        solicitud = null
        handler.removeCallbacksAndMessages(null)
        db.close()
        super.onDestroy()
    }
}
