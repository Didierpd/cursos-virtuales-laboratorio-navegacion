package com.cursosvirtuales.app;

import android.app.Activity;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.location.*;
import android.os.*;
import android.test.InstrumentationTestCase;
import com.cursosvirtuales.app.data.*;
import com.cursosvirtuales.app.odometro.*;
import java.io.*;
import java.util.concurrent.Callable;

/** Usa el Binder y un proveedor GPS de prueba del sistema, sin inyectar metros al servicio. */
public class OdometerServiceTest extends InstrumentationTestCase {
    private Context context;
    private Activity activity;
    private volatile OdometerService service;
    private LocationManager locations;
    private boolean bound;
    private boolean mockAdded;
    private String user;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((OdometerService.OdometerBinder) binder).getOdometer();
        }
        @Override public void onServiceDisconnected(ComponentName name) { service = null; }
    };

    private void shell(String command) throws Exception {
        try (ParcelFileDescriptor fd = getInstrumentation().getUiAutomation().executeShellCommand(command);
             FileInputStream stream = new FileInputStream(fd.getFileDescriptor())) {
            byte[] buffer = new byte[1024];
            while (stream.read(buffer) != -1) { }
        }
    }

    @Override protected void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getTargetContext();
        assertTrue("Usar la variante aislada", context.getPackageName().endsWith(".mapaslab"));
        user = "odometro_test_" + System.nanoTime();
        try (AppDatabaseHelper helper = new AppDatabaseHelper(context)) {
            helper.getWritableDatabase().execSQL("INSERT INTO USUARIO (_id,USERNAME,PASSWORD_HASH,ROL,NOMBRE_COMPLETO,EMAIL) SELECT ?,?,PASSWORD_HASH,'ESTUDIANTE','Estudiante de prueba','odometro@example.test' FROM USUARIO WHERE _id='u-estudiante-1'", new Object[]{user,user});
        }
        AuthRepository.INSTANCE.init(context);
        new SessionManager(context).startSession(user);
        shell("pm grant " + context.getPackageName() + " android.permission.ACCESS_FINE_LOCATION");
        shell("pm grant " + context.getPackageName() + " android.permission.ACCESS_COARSE_LOCATION");
        shell("appops set " + context.getPackageName() + " android:mock_location allow");
        locations = context.getSystemService(LocationManager.class);
        locations.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, true, true, true,
                Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
        mockAdded = true;
        locations.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
        connect();
    }

    private void connect() throws Exception {
        activity = getInstrumentation().startActivitySync(new Intent(context, OdometerActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        getInstrumentation().runOnMainSync(() -> bound = context.bindService(
                new Intent(context, OdometerService.class), connection, Context.BIND_AUTO_CREATE));
        waitFor(() -> service != null && user.equals(service.getEstado().getUsuario()));
    }

    private void waitFor(Callable<Boolean> condition) throws Exception {
        long end = SystemClock.elapsedRealtime() + 10000;
        while (SystemClock.elapsedRealtime() < end) {
            if (condition.call()) return;
            SystemClock.sleep(100);
        }
        fail("El odómetro no alcanzó el estado esperado");
    }

    private void gps(double latitude, double longitude) {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(1);
        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        locations.setTestProviderLocation(LocationManager.GPS_PROVIDER, location);
    }

    public void testGpsDistancePersistsPausesAndDoesNotBridgeUnobservedGap() throws Exception {
        getInstrumentation().runOnMainSync(() -> service.iniciar("Tutoría de Matemáticas", 5f, 1));
        waitFor(() -> service.getEstado().getRegistro() != null && "ACTIVO".equals(service.getEstado().getRegistro().getEstado()));
        long id = service.getEstado().getRegistro().getId();
        gps(4.6300, -74.0600);
        waitFor(() -> service.getEstado().getRegistro().getMuestras() >= 1);
        assertEquals(0.0, service.getDistance(), 0.01);
        SystemClock.sleep(1200);
        gps(4.6310, -74.0600);
        waitFor(() -> service.getDistance() > 100);
        double first = service.getDistance();
        assertTrue(first < 113);
        SystemClock.sleep(1200);
        gps(4.631001, -74.0600);
        SystemClock.sleep(500);
        assertEquals(first, service.getDistance(), 0.5);

        // Salir de la actividad pausa el registro incluso si el test conserva su Binder.
        getInstrumentation().runOnMainSync(() -> activity.finish());
        waitFor(() -> "PAUSADO".equals(service.getEstado().getRegistro().getEstado()));
        getInstrumentation().runOnMainSync(() -> { context.unbindService(connection); bound = false; });
        service = null;
        SystemClock.sleep(500);
        OdometerRepository repository = new OdometerRepository(context);
        assertEquals(first, repository.historial().get(0).getMetros(), 0.5);
        connect();
        assertEquals(id, service.getEstado().getRegistro().getId());
        assertEquals(first, service.getDistance(), 0.5);
        int samples = service.getEstado().getRegistro().getMuestras();
        getInstrumentation().runOnMainSync(() -> service.iniciar("Tutoría de Matemáticas", 5f, 1));
        waitFor(() -> "ACTIVO".equals(service.getEstado().getRegistro().getEstado()));
        gps(4.6500, -74.0600);
        waitFor(() -> service.getEstado().getRegistro().getMuestras() > samples);
        assertEquals(first, service.getDistance(), 0.5);
        SystemClock.sleep(1200);
        gps(4.6510, -74.0600);
        waitFor(() -> service.getDistance() > first + 100);
        double total = service.getDistance();
        getInstrumentation().runOnMainSync(() -> service.finalizar());
        waitFor(() -> service.getEstado().getRegistro() == null);
        OdometerRepository.Registro saved = repository.historial().get(0);
        assertEquals("FINALIZADO", saved.getEstado());
        assertTrue(saved.getSimulado());
        assertEquals(total, saved.getMetros(), 0.5);
        assertEquals(total, repository.resumen().get(0).getMetros(), 0.5);
        new SessionManager(context).startSession("u-estudiante-1");
        assertFalse(repository.historial().stream().anyMatch(row -> row.getId() == id));
    }

    @Override protected void tearDown() throws Exception {
        if (context != null) {
            if (activity != null) getInstrumentation().runOnMainSync(() -> activity.finish());
            if (bound) getInstrumentation().runOnMainSync(() -> context.unbindService(connection));
            SystemClock.sleep(300);
            if (mockAdded) locations.removeTestProvider(LocationManager.GPS_PROVIDER);
            shell("appops set " + context.getPackageName() + " android:mock_location default");
            try (AppDatabaseHelper helper = new AppDatabaseHelper(context)) {
                SQLiteDatabase db = helper.getWritableDatabase();
                db.delete("DESPLAZAMIENTO", "ESTUDIANTE_ID=?", new String[]{user});
                db.delete("USUARIO", "_id=?", new String[]{user});
            }
            new SessionManager(context).logout();
        }
        super.tearDown();
    }
}
