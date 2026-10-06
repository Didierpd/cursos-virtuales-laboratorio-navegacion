package com.cursosvirtuales.app.cursos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cursosvirtuales.app.data.AppDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla UNIDAD (HU-05).
 */
public class UnitRepository {

    private final AppDatabaseHelper helper;

    public UnitRepository(Context context) {
        this.helper = new AppDatabaseHelper(context.getApplicationContext());
    }

    /** Inserta una unidad al final del curso (orden = cantidad actual de unidades). */
    public long insertar(long cursoId, String nombre, String recurso) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int siguienteOrden = contarUnidades(db, cursoId);

        ContentValues valores = new ContentValues();
        valores.put(AppDatabaseHelper.COL_UNIDAD_CURSO_ID, cursoId);
        valores.put(AppDatabaseHelper.COL_UNIDAD_NOMBRE, nombre);
        valores.put(AppDatabaseHelper.COL_UNIDAD_RECURSO, recurso);
        valores.put(AppDatabaseHelper.COL_UNIDAD_ORDEN, siguienteOrden);

        long id = db.insert(AppDatabaseHelper.TABLA_UNIDAD, null, valores);
        db.close();
        return id;
    }

    /** Elimina una unidad por su id. */
    public boolean eliminar(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int filas = db.delete(
                AppDatabaseHelper.TABLA_UNIDAD,
                AppDatabaseHelper.COL_UNIDAD_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return filas > 0;
    }

    /** Unidades del curso, ordenadas por su posicion. */
    public List<CourseUnit> listarPorCurso(long cursoId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                AppDatabaseHelper.TABLA_UNIDAD,
                null,
                AppDatabaseHelper.COL_UNIDAD_CURSO_ID + " = ?",
                new String[]{String.valueOf(cursoId)},
                null, null,
                AppDatabaseHelper.COL_UNIDAD_ORDEN + " ASC");

        List<CourseUnit> unidades = new ArrayList<>();
        while (cursor.moveToNext()) {
            unidades.add(aUnidad(cursor));
        }
        cursor.close();
        db.close();
        return unidades;
    }

    /**
     * Intercambia el orden de una unidad con la unidad vecina (arriba o abajo).
     * Devuelve true si se pudo mover (es decir, no era ya el primero/ultimo).
     */
    public boolean mover(CourseUnit unidad, boolean haciaArriba) {
        List<CourseUnit> unidades = listarPorCurso(unidad.getCursoId());
        int posicionActual = -1;
        for (int i = 0; i < unidades.size(); i++) {
            if (unidades.get(i).getId() == unidad.getId()) {
                posicionActual = i;
                break;
            }
        }
        int posicionVecina = haciaArriba ? posicionActual - 1 : posicionActual + 1;
        if (posicionActual == -1 || posicionVecina < 0 || posicionVecina >= unidades.size()) {
            return false;
        }

        CourseUnit vecina = unidades.get(posicionVecina);
        int ordenActual = unidad.getOrden();
        int ordenVecina = vecina.getOrden();

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            actualizarOrden(db, unidad.getId(), ordenVecina);
            actualizarOrden(db, vecina.getId(), ordenActual);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        db.close();
        return true;
    }

    private void actualizarOrden(SQLiteDatabase db, long unidadId, int nuevoOrden) {
        ContentValues valores = new ContentValues();
        valores.put(AppDatabaseHelper.COL_UNIDAD_ORDEN, nuevoOrden);
        db.update(
                AppDatabaseHelper.TABLA_UNIDAD,
                valores,
                AppDatabaseHelper.COL_UNIDAD_ID + " = ?",
                new String[]{String.valueOf(unidadId)});
    }

    private int contarUnidades(SQLiteDatabase db, long cursoId) {
        try (Cursor cursor = db.rawQuery("SELECT COALESCE(MAX(ORDEN), -1) + 1 FROM UNIDAD WHERE CURSO_ID = ?", new String[]{String.valueOf(cursoId)})) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    private CourseUnit aUnidad(Cursor cursor) {
        return new CourseUnit(
                cursor.getLong(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_UNIDAD_ID)),
                cursor.getLong(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_UNIDAD_CURSO_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_UNIDAD_NOMBRE)),
                cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_UNIDAD_RECURSO)),
                cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_UNIDAD_ORDEN)));
    }
}
