package com.cursosvirtuales.app.servicios

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.cursosvirtuales.app.data.AppDatabaseHelper

/** El aviso y el cambio académico se guardan en la misma transacción SQLite. */
class AvisosRepository(private val context: Context) {
    data class Aviso(val id: Long, val destinatario: String, val tareaId: Long?,
        val titulo: String, val mensaje: String, val disponibleDesde: Long, val publicado: Boolean)

    fun listar(usuario: String): List<Aviso> = AppDatabaseHelper(context).use { helper ->
        helper.readableDatabase.rawQuery(
            "SELECT _id,DESTINATARIO,TAREA_ID,TITULO,MENSAJE,DISPONIBLE_DESDE,PUBLICADO FROM AVISO WHERE DESTINATARIO=? ORDER BY _id DESC",
            arrayOf(usuario)
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(Aviso(cursor.getLong(0), cursor.getString(1),
                    if (cursor.isNull(2)) null else cursor.getLong(2), cursor.getString(3), cursor.getString(4),
                    cursor.getLong(5), cursor.getInt(6) == 1))
            }
        }
    }

    fun demostracion(usuario: String, mensaje: String): Long = AppDatabaseHelper(context).use { helper ->
        guardar(helper.writableDatabase, usuario, null, "Recordatorio de Cursos Virtuales", mensaje,
            System.currentTimeMillis() + 10_000)
    }

    fun publicado(id: Long, usuario: String) {
        AppDatabaseHelper(context).use { helper ->
            helper.writableDatabase.update("AVISO", ContentValues().apply { put("PUBLICADO", 1) },
                "_id=? AND DESTINATARIO=?", arrayOf(id.toString(), usuario))
        }
    }

    companion object {
        fun guardar(db: SQLiteDatabase, usuario: String, tarea: Long?, titulo: String, mensaje: String,
                    disponibleDesde: Long = System.currentTimeMillis()): Long {
            val values = ContentValues().apply {
                put("DESTINATARIO", usuario)
                if (tarea == null) putNull("TAREA_ID") else put("TAREA_ID", tarea)
                put("TITULO", titulo)
                put("MENSAJE", mensaje)
                put("DISPONIBLE_DESDE", disponibleDesde)
            }
            return db.insertOrThrow("AVISO", null, values)
        }
    }
}
