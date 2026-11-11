package com.cursosvirtuales.app;

import android.test.AndroidTestCase;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import com.cursosvirtuales.app.talleres.encomiendas.*;
import java.io.File;

/** Comprueba transacciones y persistencia real sin tocar la base del usuario. */
public class EncomiendasIntegrationTest extends AndroidTestCase {
    private Context isolated;
    @Override protected void setUp() throws Exception {
        super.setUp();
        final String prefix = "test_enc_" + System.nanoTime() + "_";
        isolated = new ContextWrapper(getContext()) {
            @Override public File getDatabasePath(String name) { return super.getDatabasePath(prefix + name); }
            @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
                return super.openOrCreateDatabase(prefix + name, mode, factory);
            }
            @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, android.database.DatabaseErrorHandler handler) {
                return super.openOrCreateDatabase(prefix + name, mode, factory, handler);
            }
        };
    }
    @Override protected void tearDown() throws Exception {
        isolated.getDatabasePath("encomiendas.db").delete();
        super.tearDown();
    }
    private Remitente sender(String name) { return new Remitente(name, "101", "3001234567", "Campus", "Bogota"); }
    private Destinatario recipient() { return new Destinatario("Luis", "202", "3011234567", "Sede", "Bogota"); }
    private Encomienda parcel(Remitente sender) {
        return new Encomienda("TEST-001", "Material", 1.5, 50000, "Normal", "2026-10-21", sender, recipient());
    }
    public void testRegistrationSearchAndReopen() {
        EncomiendasDbHelper helper = new EncomiendasDbHelper(isolated);
        assertTrue(helper.insertarEncomienda(sender("Ana"), recipient(), parcel(sender("Ana"))) > 0);
        helper.close();
        helper = new EncomiendasDbHelper(isolated);
        assertEquals(1, helper.consultarEncomiendas().size());
        assertEquals("Ana", helper.buscarEncomiendas("TEST-001").get(0).getRemitente().getNombre());
        assertEquals(1, helper.buscarEncomiendas("101").size());
        assertEquals(1, helper.buscarEncomiendas("202").size());
        assertTrue(helper.buscarEncomiendas("' OR 1=1 --").isEmpty());
        helper.close();
    }
    public void testDuplicateRollsBackPersonUpdate() {
        EncomiendasDbHelper helper = new EncomiendasDbHelper(isolated);
        assertTrue(helper.insertarEncomienda(sender("Ana"), recipient(), parcel(sender("Ana"))) > 0);
        assertEquals(-1L, helper.insertarEncomienda(sender("No guardar"), recipient(), parcel(sender("No guardar"))));
        assertEquals(1, helper.consultarEncomiendas().size());
        assertEquals("Ana", helper.consultarEncomiendas().get(0).getRemitente().getNombre());
        helper.close();
    }
    public void testRouteAssignmentAfterVersionOneMigration() {
        EncomiendasDbHelper helper = new EncomiendasDbHelper(isolated);
        long id = helper.insertarEncomienda(sender("Ana"), recipient(), parcel(sender("Ana")));
        assertTrue(id > 0);
        SQLiteDatabase v1 = helper.getWritableDatabase();
        v1.execSQL("DROP TABLE RECOLECCION");
        v1.setVersion(1);
        helper.close();

        helper = new EncomiendasDbHelper(isolated);
        assertEquals(1, helper.consultarEncomiendas().size());
        helper.asignarRecoleccion(id, "2026-11-10", "R1", 4.63, -74.06);
        helper.asignarRecoleccion(id, "2026-11-11", "R1", 4.64, -74.07);
        helper.asignarRecoleccion(id, "2026-11-10", "R2", 4.65, -74.08);
        helper.close();

        helper = new EncomiendasDbHelper(isolated);
        try (android.database.Cursor cursor = helper.paradasDelDia("2026-11-10", "R2")) {
            assertTrue(cursor.moveToFirst());
            assertEquals("TEST-001", cursor.getString(0));
            assertEquals(4.65, cursor.getDouble(1), 0.00001);
            assertFalse(cursor.moveToNext());
        }
        try (android.database.Cursor cursor = helper.paradasDelDia("2026-11-10", "R1")) {
            assertEquals(0, cursor.getCount());
        }
        try (android.database.Cursor cursor = helper.paradasDelDia("2026-11-11", "R1")) {
            assertEquals(1, cursor.getCount());
        }
        helper.close();
    }
}
