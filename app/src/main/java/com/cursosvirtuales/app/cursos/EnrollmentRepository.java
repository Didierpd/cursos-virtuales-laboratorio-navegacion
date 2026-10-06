package com.cursosvirtuales.app.cursos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cursosvirtuales.app.data.AppDatabaseHelper;

/**
 * HU-04 - Inscribirme en un curso disponible.
 *
 * Acceso a datos de la tabla INSCRIPCION, que relaciona un estudiante con
 * un curso. La combinacion (curso, estudiante) es unica a nivel de base de
 * datos para que no se pueda inscribir dos veces al mismo curso.
 */
public class EnrollmentRepository {

    private final AppDatabaseHelper helper;

    public EnrollmentRepository(Context context) {
        this.helper = new AppDatabaseHelper(context.getApplicationContext());
    }

    /** Inscribe al estudiante en el curso. Devuelve false si ya estaba inscrito. */
    public boolean inscribir(long cursoId, String estudianteId) {
        if (estaInscrito(cursoId, estudianteId)) {
            return false;
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(AppDatabaseHelper.COL_INSCRIPCION_CURSO_ID, cursoId);
        valores.put(AppDatabaseHelper.COL_INSCRIPCION_ESTUDIANTE_ID, estudianteId);
        long id = db.insert(AppDatabaseHelper.TABLA_INSCRIPCION, null, valores);
        db.close();
        return id != -1L;
    }

    /** true si el estudiante ya esta inscrito en ese curso. */
    public boolean estaInscrito(long cursoId, String estudianteId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                AppDatabaseHelper.TABLA_INSCRIPCION,
                new String[]{AppDatabaseHelper.COL_INSCRIPCION_ID},
                AppDatabaseHelper.COL_INSCRIPCION_CURSO_ID + " = ? AND "
                        + AppDatabaseHelper.COL_INSCRIPCION_ESTUDIANTE_ID + " = ?",
                new String[]{String.valueOf(cursoId), estudianteId},
                null, null, null);
        boolean existe = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return existe;
    }
}
