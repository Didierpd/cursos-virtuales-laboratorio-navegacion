package com.cursosvirtuales.app.odometro

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.location.Location
import com.cursosvirtuales.app.data.SessionManager
import com.cursosvirtuales.app.data.AppDatabaseHelper
import com.cursosvirtuales.app.model.Role

/** Historial por estudiante. Cada muestra y su acumulado se confirman juntos. */
class OdometerRepository(private val context: Context) {
    data class Registro(val id: Long, val formacion: String, val inicio: Long, val fin: Long?,
        val estado: String, val metros: Double, val precision: Float, val intervalo: Long,
        val latitud: Double?, val longitud: Double?, val muestras: Int, val simulado: Boolean)
    data class Resumen(val formacion: String, val recorridos: Int, val metros: Double)

    fun estudiante(): String {
        val session = SessionManager(context)
        require(session.isSessionValid()) { "Ingresa con una cuenta de estudiante." }
        val id = requireNotNull(session.currentUserId())
        val esEstudiante = AppDatabaseHelper(context).use { helper ->
            helper.readableDatabase.rawQuery("SELECT ROL FROM USUARIO WHERE _id=?", arrayOf(id)).use { c ->
                c.moveToFirst() && c.getString(0) == Role.ESTUDIANTE.name
            }
        }
        require(esEstudiante) { "Ingresa con una cuenta de estudiante." }
        return id
    }

    fun historial(): List<Registro> = listar(estudiante())

    internal fun listar(usuario: String): List<Registro> = AppDatabaseHelper(context).use { helper ->
        helper.readableDatabase.rawQuery("SELECT * FROM DESPLAZAMIENTO WHERE ESTUDIANTE_ID=? ORDER BY _id DESC",
            arrayOf(usuario)).use { c -> buildList { while (c.moveToNext()) add(leer(c)) } }
    }

    fun resumen(): List<Resumen> = AppDatabaseHelper(context).use { helper ->
        helper.readableDatabase.rawQuery("SELECT FORMACION,COUNT(*),SUM(METROS) FROM DESPLAZAMIENTO WHERE ESTUDIANTE_ID=? GROUP BY FORMACION ORDER BY FORMACION",
            arrayOf(estudiante())).use { c ->
            buildList { while (c.moveToNext()) add(Resumen(c.getString(0), c.getInt(1), c.getDouble(2))) }
        }
    }

    internal fun recuperar(usuario: String): Registro? = AppDatabaseHelper(context).use { helper ->
        helper.writableDatabase.execSQL("UPDATE DESPLAZAMIENTO SET ESTADO='PAUSADO',SEGMENTO=NULL WHERE ESTUDIANTE_ID=? AND ESTADO='ACTIVO'", arrayOf(usuario))
        listar(usuario).firstOrNull { it.estado != "FINALIZADO" }
    }

    internal fun iniciar(usuario: String, formacion: String, precision: Float, intervalo: Long, segmento: String): Registro {
        require(formacion.isNotBlank()) { "Indica el proceso formativo o la tutoría." }
        require(precision.isFinite() && precision > 0) { "La precisión del movimiento debe ser positiva." }
        require(intervalo > 0 && intervalo <= Long.MAX_VALUE / 1000) { "El intervalo debe ser un número positivo de segundos." }
        return AppDatabaseHelper(context).use { helper ->
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                val anterior = db.rawQuery("SELECT * FROM DESPLAZAMIENTO WHERE ESTUDIANTE_ID=? AND ESTADO!='FINALIZADO'", arrayOf(usuario)).use { c ->
                    if (c.moveToFirst()) leer(c) else null
                }
                val values = ContentValues().apply {
                    put("ESTADO", "ACTIVO")
                    put("SEGMENTO", segmento)
                }
                if (anterior == null) {
                    values.put("ESTUDIANTE_ID", usuario)
                    values.put("FORMACION", formacion.trim())
                    values.put("INICIO", System.currentTimeMillis())
                    values.put("PRECISION_MOVIMIENTO", precision)
                    values.put("INTERVALO_SEGUNDOS", intervalo)
                    db.insertOrThrow("DESPLAZAMIENTO", null, values)
                } else {
                    db.update("DESPLAZAMIENTO", values, "_id=?", arrayOf(anterior.id.toString()))
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
            listar(usuario).first { it.estado == "ACTIVO" }
        }
    }

    internal fun muestra(id: Long, usuario: String, segmento: String, location: Location, metros: Double): Boolean {
        require(metros.isFinite() && metros >= 0)
        return AppDatabaseHelper(context).use { helper ->
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                val autorizado = db.rawQuery("SELECT _id FROM DESPLAZAMIENTO WHERE _id=? AND ESTUDIANTE_ID=? AND ESTADO='ACTIVO' AND SEGMENTO=?",
                    arrayOf(id.toString(), usuario, segmento)).use { it.moveToFirst() }
                if (!autorizado) return@use false
                @Suppress("DEPRECATION") val simulado = location.isFromMockProvider
                val sample = ContentValues().apply {
                    put("DESPLAZAMIENTO_ID", id)
                    put("FECHA", location.time)
                    put("LATITUD", location.latitude)
                    put("LONGITUD", location.longitude)
                    put("PRECISION_GPS", location.accuracy)
                    put("METROS_TRAMO", metros)
                    put("SIMULADO", if (simulado) 1 else 0)
                }
                db.insertOrThrow("PUNTO_RECORRIDO", null, sample)
                db.execSQL("UPDATE DESPLAZAMIENTO SET METROS=METROS+?,LATITUD=?,LONGITUD=?,MUESTRAS=MUESTRAS+1,SIMULADO=MAX(SIMULADO,?) WHERE _id=?",
                    arrayOf(metros, location.latitude, location.longitude, if (simulado) 1 else 0, id))
                db.setTransactionSuccessful()
                true
            } finally { db.endTransaction() }
        }
    }

    internal fun pausar(id: Long, usuario: String, segmento: String) {
        AppDatabaseHelper(context).use { helper ->
            helper.writableDatabase.execSQL("UPDATE DESPLAZAMIENTO SET ESTADO='PAUSADO',SEGMENTO=NULL WHERE _id=? AND ESTUDIANTE_ID=? AND SEGMENTO=?",
                arrayOf(id, usuario, segmento))
        }
    }

    internal fun finalizar(id: Long, usuario: String) {
        AppDatabaseHelper(context).use { helper ->
            helper.writableDatabase.execSQL("UPDATE DESPLAZAMIENTO SET ESTADO='FINALIZADO',SEGMENTO=NULL,FIN=? WHERE _id=? AND ESTUDIANTE_ID=?",
                arrayOf(System.currentTimeMillis(), id, usuario))
        }
    }

    private fun leer(c: Cursor): Registro {
        fun col(nombre: String) = c.getColumnIndexOrThrow(nombre)
        fun coordenada(nombre: String) = if (c.isNull(col(nombre))) null else c.getDouble(col(nombre))
        return Registro(c.getLong(col("_id")), c.getString(col("FORMACION")), c.getLong(col("INICIO")),
            if (c.isNull(col("FIN"))) null else c.getLong(col("FIN")), c.getString(col("ESTADO")),
            c.getDouble(col("METROS")), c.getFloat(col("PRECISION_MOVIMIENTO")), c.getLong(col("INTERVALO_SEGUNDOS")),
            coordenada("LATITUD"), coordenada("LONGITUD"), c.getInt(col("MUESTRAS")), c.getInt(col("SIMULADO")) == 1)
    }
}
