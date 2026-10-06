package com.cursosvirtuales.app.cursos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cursosvirtuales.app.data.AppDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla CURSO.
 *
 * Concentra el SQL para que las Activities no manipulen la base de datos
 * directamente. El id del docente siempre se recibe desde afuera (nunca
 * hardcodeado), leido de la sesion activa por quien llama.
 */
public class CourseRepository {

    private final AppDatabaseHelper helper;

    public CourseRepository(Context context) {
        this.helper = new AppDatabaseHelper(context.getApplicationContext());
    }

    /** Inserta un curso nuevo y devuelve el id generado, o -1 si falla. */
    public long insertar(Course curso) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insert(AppDatabaseHelper.TABLA_CURSO, null, aContentValues(curso));
        db.close();
        return id;
    }

    /** Actualiza un curso existente. Devuelve true si se modifico una fila. */
    public boolean actualizar(Course curso) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int filas = db.update(
                AppDatabaseHelper.TABLA_CURSO,
                aContentValues(curso),
                AppDatabaseHelper.COL_CURSO_ID + " = ?",
                new String[]{String.valueOf(curso.getId())});
        db.close();
        return filas > 0;
    }

    /** Elimina un curso por su id. */
    public boolean eliminar(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int filas = db.delete(
                AppDatabaseHelper.TABLA_CURSO,
                AppDatabaseHelper.COL_CURSO_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return filas > 0;
    }

    /** Cursos del docente en sesion, ordenados por fecha de inicio. */
    public List<Course> listarPorDocente(String docenteId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                AppDatabaseHelper.TABLA_CURSO,
                null,
                AppDatabaseHelper.COL_CURSO_DOCENTE_ID + " = ?",
                new String[]{docenteId},
                null, null,
                AppDatabaseHelper.COL_CURSO_FECHA_INICIO + " ASC");

        List<Course> cursos = cursorACursos(cursor);
        db.close();
        return cursos;
    }

    /** HU-04: todos los cursos de la plataforma (de cualquier docente), para que un estudiante los explore. */
    public List<Course> listarTodos() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                AppDatabaseHelper.TABLA_CURSO,
                null, null, null, null, null,
                AppDatabaseHelper.COL_CURSO_FECHA_INICIO + " ASC");

        List<Course> cursos = cursorACursos(cursor);
        db.close();
        return cursos;
    }

    /** Busca un curso por id. Devuelve null si no existe. */
    public Course buscarPorId(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                AppDatabaseHelper.TABLA_CURSO,
                null,
                AppDatabaseHelper.COL_CURSO_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null);

        Course curso = null;
        if (cursor.moveToFirst()) {
            curso = aCurso(cursor);
        }
        cursor.close();
        db.close();
        return curso;
    }

    /**
     * Indica si el docente ya tiene un curso con ese nombre.
     * El parametro idExcluido permite editar un curso sin chocar consigo mismo.
     */
    public boolean existeNombre(String nombre, String docenteId, long idExcluido) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                AppDatabaseHelper.TABLA_CURSO,
                new String[]{AppDatabaseHelper.COL_CURSO_ID},
                AppDatabaseHelper.COL_CURSO_NOMBRE + " = ? AND "
                        + AppDatabaseHelper.COL_CURSO_DOCENTE_ID + " = ? AND "
                        + AppDatabaseHelper.COL_CURSO_ID + " <> ?",
                new String[]{nombre, docenteId, String.valueOf(idExcluido)},
                null, null, null);

        boolean existe = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return existe;
    }

    private List<Course> cursorACursos(Cursor cursor) {
        List<Course> cursos = new ArrayList<>();
        while (cursor.moveToNext()) {
            cursos.add(aCurso(cursor));
        }
        cursor.close();
        return cursos;
    }

    private ContentValues aContentValues(Course curso) {
        ContentValues valores = new ContentValues();
        valores.put(AppDatabaseHelper.COL_CURSO_NOMBRE, curso.getNombre());
        valores.put(AppDatabaseHelper.COL_CURSO_DESCRIPCION, curso.getDescripcion());
        valores.put(AppDatabaseHelper.COL_CURSO_FECHA_INICIO, curso.getFechaInicio());
        valores.put(AppDatabaseHelper.COL_CURSO_FECHA_FIN, curso.getFechaFin());
        valores.put(AppDatabaseHelper.COL_CURSO_DOCENTE_ID, curso.getDocenteId());
        return valores;
    }

    private Course aCurso(Cursor cursor) {
        return new Course(
                cursor.getLong(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CURSO_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CURSO_NOMBRE)),
                cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CURSO_DESCRIPCION)),
                cursor.getLong(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CURSO_FECHA_INICIO)),
                cursor.getLong(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CURSO_FECHA_FIN)),
                cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CURSO_DOCENTE_ID)));
    }
}
