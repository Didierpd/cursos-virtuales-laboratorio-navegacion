package com.cursosvirtuales.app.cursos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cursosvirtuales.app.data.AppDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla TAREA (HU-06).
 */
public class TaskRepository {

    private final AppDatabaseHelper helper;

    public TaskRepository(Context context) {
        this.helper = new AppDatabaseHelper(context.getApplicationContext());
    }

    /** Inserta una tarea nueva y devuelve el id generado, o -1 si falla. */
    public long insertar(CourseTask tarea) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insert(AppDatabaseHelper.TABLA_TAREA, null, aContentValues(tarea));
        db.close();
        return id;
    }

    /** Elimina una tarea por su id. */
    public boolean eliminar(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int filas = db.delete(
                AppDatabaseHelper.TABLA_TAREA,
                AppDatabaseHelper.COL_TAREA_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return filas > 0;
    }

    /** Tareas del curso, ordenadas por fecha limite. */
    public List<CourseTask> listarPorCurso(long cursoId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                AppDatabaseHelper.TABLA_TAREA,
                null,
                AppDatabaseHelper.COL_TAREA_CURSO_ID + " = ?",
                new String[]{String.valueOf(cursoId)},
                null, null,
                AppDatabaseHelper.COL_TAREA_FECHA_LIMITE + " ASC");

        List<CourseTask> tareas = new ArrayList<>();
        while (cursor.moveToNext()) {
            tareas.add(aTarea(cursor));
        }
        cursor.close();
        db.close();
        return tareas;
    }

    private ContentValues aContentValues(CourseTask tarea) {
        ContentValues valores = new ContentValues();
        valores.put(AppDatabaseHelper.COL_TAREA_CURSO_ID, tarea.getCursoId());
        valores.put(AppDatabaseHelper.COL_TAREA_TITULO, tarea.getTitulo());
        valores.put(AppDatabaseHelper.COL_TAREA_ENUNCIADO, tarea.getEnunciado());
        valores.put(AppDatabaseHelper.COL_TAREA_FECHA_LIMITE, tarea.getFechaLimite());
        valores.put(AppDatabaseHelper.COL_TAREA_PUNTAJE, tarea.getPuntaje());
        return valores;
    }

    private CourseTask aTarea(Cursor cursor) {
        return new CourseTask(
                cursor.getLong(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TAREA_ID)),
                cursor.getLong(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TAREA_CURSO_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TAREA_TITULO)),
                cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TAREA_ENUNCIADO)),
                cursor.getLong(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TAREA_FECHA_LIMITE)),
                cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TAREA_PUNTAJE)));
    }
}
