package com.cursosvirtuales.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.MessageDigest;

/**
 * Base de datos local unica de la aplicacion (SQLite nativo de Android).
 *
 * Todas las tablas viven en el mismo archivo para poder relacionarlas entre
 * si (por ejemplo, INSCRIPCION referencia tanto a USUARIO como a CURSO).
 * No requiere dependencias adicionales en build.gradle.kts porque
 * android.database.sqlite hace parte del framework.
 */
// Explicit Closeable keeps Kotlin use() compatible with Android API 24–28.
public class AppDatabaseHelper extends SQLiteOpenHelper implements java.io.Closeable {

    private static final String NOMBRE_BD = "cursos_virtuales.db";
    private static final int VERSION_BD = 6;

    // Tabla USUARIO (login/perfil/registro)
    public static final String TABLA_USUARIO = "USUARIO";
    public static final String COL_USUARIO_ID = "_id";
    public static final String COL_USUARIO_USERNAME = "USERNAME";
    public static final String COL_USUARIO_PASSWORD_HASH = "PASSWORD_HASH";
    public static final String COL_USUARIO_ROL = "ROL";
    public static final String COL_USUARIO_NOMBRE = "NOMBRE_COMPLETO";
    public static final String COL_USUARIO_EMAIL = "EMAIL";
    public static final String COL_USUARIO_FOTO_URI = "FOTO_URI";

    // Tabla CURSO (HU-03)
    public static final String TABLA_CURSO = "CURSO";
    public static final String COL_CURSO_ID = "_id";
    public static final String COL_CURSO_NOMBRE = "NOMBRE";
    public static final String COL_CURSO_DESCRIPCION = "DESCRIPCION";
    public static final String COL_CURSO_FECHA_INICIO = "FECHA_INICIO";
    public static final String COL_CURSO_FECHA_FIN = "FECHA_FIN";
    public static final String COL_CURSO_DOCENTE_ID = "DOCENTE_ID";

    // Tabla INSCRIPCION (HU-04)
    public static final String TABLA_INSCRIPCION = "INSCRIPCION";
    public static final String COL_INSCRIPCION_ID = "_id";
    public static final String COL_INSCRIPCION_CURSO_ID = "CURSO_ID";
    public static final String COL_INSCRIPCION_ESTUDIANTE_ID = "ESTUDIANTE_ID";

    // Tabla UNIDAD (HU-05)
    public static final String TABLA_UNIDAD = "UNIDAD";
    public static final String COL_UNIDAD_ID = "_id";
    public static final String COL_UNIDAD_CURSO_ID = "CURSO_ID";
    public static final String COL_UNIDAD_NOMBRE = "NOMBRE";
    public static final String COL_UNIDAD_RECURSO = "RECURSO";
    public static final String COL_UNIDAD_ORDEN = "ORDEN";

    // Tabla TAREA (HU-06)
    public static final String TABLA_TAREA = "TAREA";
    public static final String COL_TAREA_ID = "_id";
    public static final String COL_TAREA_CURSO_ID = "CURSO_ID";
    public static final String COL_TAREA_TITULO = "TITULO";
    public static final String COL_TAREA_ENUNCIADO = "ENUNCIADO";
    public static final String COL_TAREA_FECHA_LIMITE = "FECHA_LIMITE";
    public static final String COL_TAREA_PUNTAJE = "PUNTAJE";

    public AppDatabaseHelper(Context context) {
        super(context, NOMBRE_BD, null, VERSION_BD);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLA_USUARIO + " ("
                + COL_USUARIO_ID + " TEXT PRIMARY KEY, "
                + COL_USUARIO_USERNAME + " TEXT NOT NULL UNIQUE, "
                + COL_USUARIO_PASSWORD_HASH + " TEXT NOT NULL, "
                + COL_USUARIO_ROL + " TEXT NOT NULL, "
                + COL_USUARIO_NOMBRE + " TEXT NOT NULL, "
                + COL_USUARIO_EMAIL + " TEXT NOT NULL, "
                + COL_USUARIO_FOTO_URI + " TEXT);");

        db.execSQL("CREATE TABLE " + TABLA_CURSO + " ("
                + COL_CURSO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_CURSO_NOMBRE + " TEXT NOT NULL, "
                + COL_CURSO_DESCRIPCION + " TEXT NOT NULL, "
                + COL_CURSO_FECHA_INICIO + " INTEGER NOT NULL, "
                + COL_CURSO_FECHA_FIN + " INTEGER NOT NULL, "
                + COL_CURSO_DOCENTE_ID + " TEXT NOT NULL);");

        db.execSQL("CREATE TABLE " + TABLA_INSCRIPCION + " ("
                + COL_INSCRIPCION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_INSCRIPCION_CURSO_ID + " INTEGER NOT NULL, "
                + COL_INSCRIPCION_ESTUDIANTE_ID + " TEXT NOT NULL, "
                + "UNIQUE(" + COL_INSCRIPCION_CURSO_ID + ", " + COL_INSCRIPCION_ESTUDIANTE_ID + "));");

        db.execSQL("CREATE TABLE " + TABLA_UNIDAD + " ("
                + COL_UNIDAD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_UNIDAD_CURSO_ID + " INTEGER NOT NULL, "
                + COL_UNIDAD_NOMBRE + " TEXT NOT NULL, "
                + COL_UNIDAD_RECURSO + " TEXT, "
                + COL_UNIDAD_ORDEN + " INTEGER NOT NULL);");

        db.execSQL("CREATE TABLE " + TABLA_TAREA + " ("
                + COL_TAREA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TAREA_CURSO_ID + " INTEGER NOT NULL, "
                + COL_TAREA_TITULO + " TEXT NOT NULL, "
                + COL_TAREA_ENUNCIADO + " TEXT NOT NULL, "
                + COL_TAREA_FECHA_LIMITE + " INTEGER NOT NULL, "
                + COL_TAREA_PUNTAJE + " INTEGER NOT NULL);");

        crearEntregas(db);
        crearPoliticaTardias(db);
        crearLaboratorios(db);
        seedUsuariosDemo(db);
    }

    /** Usuarios de demostracion (los mismos que antes vivian en memoria). */
    private void seedUsuariosDemo(SQLiteDatabase db) {
        insertarUsuario(db, "u-docente-1", "docente", hash("docente123"),
                "DOCENTE", "Ana Martínez", "ana.martinez@cursosvirtuales.edu");
        insertarUsuario(db, "u-estudiante-1", "estudiante", hash("estudiante123"),
                "ESTUDIANTE", "Luis Gómez", "luis.gomez@cursosvirtuales.edu");
    }

    private void insertarUsuario(SQLiteDatabase db, String id, String username,
                                  String passwordHash, String rol, String nombre, String email) {
        ContentValues valores = new ContentValues();
        valores.put(COL_USUARIO_ID, id);
        valores.put(COL_USUARIO_USERNAME, username);
        valores.put(COL_USUARIO_PASSWORD_HASH, passwordHash);
        valores.put(COL_USUARIO_ROL, rol);
        valores.put(COL_USUARIO_NOMBRE, nombre);
        valores.put(COL_USUARIO_EMAIL, email);
        db.insert(TABLA_USUARIO, null, valores);
    }

    /** Mismo algoritmo que AuthRepository.hash(), duplicado aqui para poder sembrar datos en onCreate. */
    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void crearEntregas(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS ENTREGA (_id INTEGER PRIMARY KEY AUTOINCREMENT, TAREA_ID INTEGER NOT NULL, ESTUDIANTE_ID TEXT NOT NULL, FECHA INTEGER NOT NULL, NOTA REAL, COMENTARIO TEXT, UNIQUE(TAREA_ID, ESTUDIANTE_ID))");
        db.execSQL("CREATE TABLE IF NOT EXISTS ADJUNTO (_id INTEGER PRIMARY KEY AUTOINCREMENT, ENTREGA_ID INTEGER NOT NULL, NOMBRE TEXT NOT NULL, RUTA TEXT NOT NULL, MIME TEXT NOT NULL)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS borrar_adjuntos AFTER DELETE ON ENTREGA BEGIN DELETE FROM ADJUNTO WHERE ENTREGA_ID=OLD._id; END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS borrar_entregas AFTER DELETE ON TAREA BEGIN DELETE FROM ENTREGA WHERE TAREA_ID=OLD._id; END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS borrar_curso AFTER DELETE ON CURSO BEGIN DELETE FROM TAREA WHERE CURSO_ID=OLD._id; DELETE FROM UNIDAD WHERE CURSO_ID=OLD._id; DELETE FROM INSCRIPCION WHERE CURSO_ID=OLD._id; END");
    }

    private void crearLaboratorios(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS LABORATORIO (_id INTEGER PRIMARY KEY AUTOINCREMENT, CURSO_ID INTEGER NOT NULL, TITULO TEXT NOT NULL, OBJETIVO TEXT NOT NULL, GUIA TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS LAB_RECURSO (_id INTEGER PRIMARY KEY AUTOINCREMENT, LAB_ID INTEGER NOT NULL, NOMBRE TEXT NOT NULL, RUTA TEXT NOT NULL, MIME TEXT NOT NULL)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS borrar_laboratorios AFTER DELETE ON CURSO BEGIN DELETE FROM LABORATORIO WHERE CURSO_ID=OLD._id; END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS borrar_recursos_lab AFTER DELETE ON LABORATORIO BEGIN DELETE FROM LAB_RECURSO WHERE LAB_ID=OLD._id; END");
    }

    private void agregarColumna(SQLiteDatabase db, String tabla, String columna, String tipo) {
        try (android.database.Cursor c = db.rawQuery("PRAGMA table_info(" + tabla + ")", null)) {
            while (c.moveToNext()) if (columna.equals(c.getString(1))) return;
        }
        db.execSQL("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + tipo);
    }

    private void crearPoliticaTardias(SQLiteDatabase db) {
        agregarColumna(db, "CURSO", "ACEPTA_TARDIAS", "INTEGER NOT NULL DEFAULT 0");
        agregarColumna(db, "ENTREGA", "DIFERENCIA_MS", "INTEGER NOT NULL DEFAULT 0");
        db.execSQL("UPDATE ENTREGA SET DIFERENCIA_MS=FECHA-COALESCE((SELECT FECHA_LIMITE FROM TAREA WHERE _id=ENTREGA.TAREA_ID), FECHA)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int anterior, int nueva) {
        // Migraciones aditivas: conservar usuarios, cursos e inscripciones existentes.
        db.execSQL("CREATE TABLE IF NOT EXISTS INSCRIPCION (_id INTEGER PRIMARY KEY AUTOINCREMENT, CURSO_ID INTEGER NOT NULL, ESTUDIANTE_ID TEXT NOT NULL, UNIQUE(CURSO_ID, ESTUDIANTE_ID))");
        db.execSQL("CREATE TABLE IF NOT EXISTS UNIDAD (_id INTEGER PRIMARY KEY AUTOINCREMENT, CURSO_ID INTEGER NOT NULL, NOMBRE TEXT NOT NULL, RECURSO TEXT, ORDEN INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS TAREA (_id INTEGER PRIMARY KEY AUTOINCREMENT, CURSO_ID INTEGER NOT NULL, TITULO TEXT NOT NULL, ENUNCIADO TEXT NOT NULL, FECHA_LIMITE INTEGER NOT NULL, PUNTAJE INTEGER NOT NULL)");
        if (anterior < 4) crearEntregas(db);
        if (anterior < 5) crearPoliticaTardias(db);
        if (anterior < 6) crearLaboratorios(db);
    }
}
