package com.cursosvirtuales.app.talleres.encomiendas;

import com.cursosvirtuales.app.R;
import com.cursosvirtuales.app.talleres.TallerActivity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper de SQLite: crea y administra la base de datos "encomiendas.db".
 *
 * Modelo relacional:
 *   REMITENTE (1) ----< ENCOMIENDA >---- (1) DESTINATARIO
 */
public class EncomiendasDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "encomiendas.db";
    private static final int DB_VERSION = 1;

    // ---------- Tablas ----------
    public static final String TABLA_REMITENTE = "REMITENTE";
    public static final String TABLA_DESTINATARIO = "DESTINATARIO";
    public static final String TABLA_ENCOMIENDA = "ENCOMIENDA";

    // ---------- Columnas comunes (REMITENTE y DESTINATARIO) ----------
    public static final String COL_ID = "_id";
    public static final String COL_NOMBRE = "NOMBRE";
    public static final String COL_DOCUMENTO = "DOCUMENTO";
    public static final String COL_TELEFONO = "TELEFONO";
    public static final String COL_DIRECCION = "DIRECCION";
    public static final String COL_CIUDAD = "CIUDAD";

    // ---------- Columnas de ENCOMIENDA ----------
    public static final String ENC_GUIA = "GUIA";
    public static final String ENC_DESCRIPCION = "DESCRIPCION";
    public static final String ENC_PESO = "PESO_KG";
    public static final String ENC_VALOR = "VALOR_DECLARADO";
    public static final String ENC_TIPO = "TIPO_ENVIO";
    public static final String ENC_FECHA = "FECHA_REGISTRO";
    public static final String ENC_REMITENTE_ID = "REMITENTE_ID";
    public static final String ENC_DESTINATARIO_ID = "DESTINATARIO_ID";

    private static String sqlCrearPersona(String tabla) {
        return "CREATE TABLE " + tabla + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_NOMBRE + " TEXT NOT NULL, "
                + COL_DOCUMENTO + " TEXT NOT NULL UNIQUE, "
                + COL_TELEFONO + " TEXT, "
                + COL_DIRECCION + " TEXT, "
                + COL_CIUDAD + " TEXT);";
    }

    private static final String SQL_CREAR_ENCOMIENDA =
            "CREATE TABLE " + TABLA_ENCOMIENDA + " ("
                    + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ENC_GUIA + " TEXT NOT NULL UNIQUE, "
                    + ENC_DESCRIPCION + " TEXT NOT NULL, "
                    + ENC_PESO + " REAL NOT NULL, "
                    + ENC_VALOR + " REAL, "
                    + ENC_TIPO + " TEXT, "
                    + ENC_FECHA + " TEXT, "
                    + ENC_REMITENTE_ID + " INTEGER NOT NULL REFERENCES "
                    + TABLA_REMITENTE + "(" + COL_ID + "), "
                    + ENC_DESTINATARIO_ID + " INTEGER NOT NULL REFERENCES "
                    + TABLA_DESTINATARIO + "(" + COL_ID + "));";

    // Consulta base: une la encomienda con su remitente y su destinatario
    private static final String SQL_SELECT_ENCOMIENDAS =
            "SELECT e." + COL_ID + ", e." + ENC_GUIA + ", e." + ENC_DESCRIPCION + ", "
                    + "e." + ENC_PESO + ", e." + ENC_VALOR + ", e." + ENC_TIPO + ", e." + ENC_FECHA + ", "
                    + "r." + COL_NOMBRE + " AS R_NOMBRE, r." + COL_DOCUMENTO + " AS R_DOC, "
                    + "r." + COL_TELEFONO + " AS R_TEL, r." + COL_DIRECCION + " AS R_DIR, "
                    + "r." + COL_CIUDAD + " AS R_CIUDAD, "
                    + "d." + COL_NOMBRE + " AS D_NOMBRE, d." + COL_DOCUMENTO + " AS D_DOC, "
                    + "d." + COL_TELEFONO + " AS D_TEL, d." + COL_DIRECCION + " AS D_DIR, "
                    + "d." + COL_CIUDAD + " AS D_CIUDAD "
                    + "FROM " + TABLA_ENCOMIENDA + " e "
                    + "JOIN " + TABLA_REMITENTE + " r ON e." + ENC_REMITENTE_ID + " = r." + COL_ID + " "
                    + "JOIN " + TABLA_DESTINATARIO + " d ON e." + ENC_DESTINATARIO_ID + " = d." + COL_ID;

    public EncomiendasDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true); // activa las llaves foráneas
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(sqlCrearPersona(TABLA_REMITENTE));
        db.execSQL(sqlCrearPersona(TABLA_DESTINATARIO));
        db.execSQL(SQL_CREAR_ENCOMIENDA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Primero la tabla hija (tiene las llaves foráneas), luego las padres
        db.execSQL("DROP TABLE IF EXISTS " + TABLA_ENCOMIENDA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLA_REMITENTE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLA_DESTINATARIO);
        onCreate(db);
    }

    // =====================================================================
    //  ADICIÓN DE DATOS
    // =====================================================================

    /**
     * Guarda remitente, destinatario y encomienda en una sola transacción.
     * Si el remitente o el destinatario ya existen (mismo documento), se
     * reutiliza su registro y se actualizan sus datos.
     *
     * @return el _id de la encomienda insertada, o -1 si algo falla.
     */
    public long insertarEncomienda(Remitente rem, Destinatario des, Encomienda enc) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long remId = guardarPersona(db, TABLA_REMITENTE, rem);
            long desId = guardarPersona(db, TABLA_DESTINATARIO, des);
            if (remId == -1 || desId == -1) {
                return -1; // endTransaction() sin éxito => rollback
            }

            ContentValues valores = new ContentValues();
            valores.put(ENC_GUIA, enc.getGuia());
            valores.put(ENC_DESCRIPCION, enc.getDescripcion());
            valores.put(ENC_PESO, enc.getPesoKg());
            valores.put(ENC_VALOR, enc.getValorDeclarado());
            valores.put(ENC_TIPO, enc.getTipoEnvio());
            valores.put(ENC_FECHA, enc.getFechaRegistro());
            valores.put(ENC_REMITENTE_ID, remId);
            valores.put(ENC_DESTINATARIO_ID, desId);

            long id = db.insert(TABLA_ENCOMIENDA, null, valores);
            if (id != -1) {
                db.setTransactionSuccessful(); // confirma (commit)
            }
            return id;
        } finally {
            db.endTransaction();
        }
    }

    /** Inserta la persona o, si ya existe su documento, la actualiza y devuelve su id. */
    private long guardarPersona(SQLiteDatabase db, String tabla, Persona p) {
        ContentValues valores = new ContentValues();
        valores.put(COL_NOMBRE, p.getNombre());
        valores.put(COL_DOCUMENTO, p.getDocumento());
        valores.put(COL_TELEFONO, p.getTelefono());
        valores.put(COL_DIRECCION, p.getDireccion());
        valores.put(COL_CIUDAD, p.getCiudad());

        Cursor c = db.query(tabla, new String[]{COL_ID},
                COL_DOCUMENTO + " = ?", new String[]{p.getDocumento()},
                null, null, null);
        try {
            if (c.moveToFirst()) {
                long id = c.getLong(0);
                db.update(tabla, valores, COL_ID + " = ?", new String[]{String.valueOf(id)});
                return id;
            }
        } finally {
            c.close();
        }
        return db.insert(tabla, null, valores);
    }

    // =====================================================================
    //  CONSULTA DE DATOS
    // =====================================================================

    /** Devuelve todas las encomiendas (la más reciente primero). */
    public List<Encomienda> consultarEncomiendas() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(SQL_SELECT_ENCOMIENDAS + " ORDER BY e." + COL_ID + " DESC", null);
        return leerCursor(c);
    }

    /** Busca encomiendas por número de guía o por documento de remitente/destinatario. */
    public List<Encomienda> buscarEncomiendas(String texto) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = SQL_SELECT_ENCOMIENDAS
                + " WHERE e." + ENC_GUIA + " = ? OR r." + COL_DOCUMENTO + " = ? OR d." + COL_DOCUMENTO + " = ?"
                + " ORDER BY e." + COL_ID + " DESC";
        // Parámetros con "?" => evita inyección SQL
        Cursor c = db.rawQuery(sql, new String[]{texto, texto, texto});
        return leerCursor(c);
    }

    private List<Encomienda> leerCursor(Cursor c) {
        List<Encomienda> lista = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                Remitente rem = new Remitente(
                        c.getString(c.getColumnIndexOrThrow("R_NOMBRE")),
                        c.getString(c.getColumnIndexOrThrow("R_DOC")),
                        c.getString(c.getColumnIndexOrThrow("R_TEL")),
                        c.getString(c.getColumnIndexOrThrow("R_DIR")),
                        c.getString(c.getColumnIndexOrThrow("R_CIUDAD")));
                Destinatario des = new Destinatario(
                        c.getString(c.getColumnIndexOrThrow("D_NOMBRE")),
                        c.getString(c.getColumnIndexOrThrow("D_DOC")),
                        c.getString(c.getColumnIndexOrThrow("D_TEL")),
                        c.getString(c.getColumnIndexOrThrow("D_DIR")),
                        c.getString(c.getColumnIndexOrThrow("D_CIUDAD")));
                Encomienda enc = new Encomienda(
                        c.getString(c.getColumnIndexOrThrow(ENC_GUIA)),
                        c.getString(c.getColumnIndexOrThrow(ENC_DESCRIPCION)),
                        c.getDouble(c.getColumnIndexOrThrow(ENC_PESO)),
                        c.getDouble(c.getColumnIndexOrThrow(ENC_VALOR)),
                        c.getString(c.getColumnIndexOrThrow(ENC_TIPO)),
                        c.getString(c.getColumnIndexOrThrow(ENC_FECHA)),
                        rem, des);
                enc.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
                lista.add(enc);
            }
        } finally {
            c.close();
        }
        return lista;
    }
}
