package com.cursosvirtuales.app;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.test.InstrumentationTestCase;
import com.cursosvirtuales.app.data.AuthRepository;
import com.cursosvirtuales.app.data.AppDatabaseHelper;
import com.cursosvirtuales.app.data.SessionManager;
import com.cursosvirtuales.app.servicios.AvisosActivity;
import com.cursosvirtuales.app.servicios.AvisosRepository;
import com.cursosvirtuales.app.servicios.DelayedMessageService;
import java.util.ArrayList;
import java.util.List;

/** Ejecutar en la variante aislada. Comprueba el servicio real y sus PendingIntent. */
public class StartedServiceTest extends InstrumentationTestCase {
    private Context context;
    private Activity activity;
    private final List<Long> created = new ArrayList<>();
    private final String teacher = "u-docente-1", student = "u-estudiante-1";

    @Override protected void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getTargetContext();
        assertTrue("Usar -Pmapas.isolatedTest=true", context.getPackageName().endsWith(".mapaslab"));
        AuthRepository.INSTANCE.init(context);
        new SessionManager(context).startSession(teacher);
        activity = getInstrumentation().startActivitySync(new Intent(context, AvisosActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        getInstrumentation().waitForIdleSync();
    }

    @Override protected void tearDown() throws Exception {
        context.stopService(new Intent(context, DelayedMessageService.class));
        context.getSystemService(NotificationManager.class).cancelAll();
        try (AppDatabaseHelper helper = new AppDatabaseHelper(context)) {
            for (long id : created) helper.getWritableDatabase().delete("AVISO", "_id=?", new String[]{String.valueOf(id)});
        }
        if (activity != null) getInstrumentation().runOnMainSync(() -> activity.finish());
        new SessionManager(context).logout();
        super.tearDown();
    }

    private StatusBarNotification posted(long id) {
        for (StatusBarNotification n : context.getSystemService(NotificationManager.class).getActiveNotifications()) {
            if (("aviso_" + id).equals(n.getTag())) return n;
        }
        return null;
    }

    private StatusBarNotification waitFor(long id, long timeout) {
        long end = SystemClock.elapsedRealtime() + timeout;
        do {
            StatusBarNotification notification = posted(id);
            if (notification != null) return notification;
            SystemClock.sleep(100);
        } while (SystemClock.elapsedRealtime() < end);
        fail("El aviso no llegó a la barra");
        return null;
    }

    private void start(String user, long demo) {
        Intent intent = new Intent(context, DelayedMessageService.class)
                .putExtra("destinatario", user).putExtra("demostracion", demo);
        getInstrumentation().runOnMainSync(() -> context.startService(intent));
    }

    public void testDelayedNotificationSurvivesLeavingActivityAndOpensApp() throws Exception {
        AvisosRepository repo = new AvisosRepository(context);
        long id = repo.demostracion(teacher, "Recordatorio del laboratorio");
        created.add(id);
        start(teacher, id);
        // Cambiar el foco reproduce el paso de la presentación sin bloquear la UI.
        getInstrumentation().runOnMainSync(() -> activity.startActivity(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        SystemClock.sleep(1000);
        assertNull(posted(id));
        StatusBarNotification notification = waitFor(id, 20000);
        assertEquals("Recordatorio de Cursos Virtuales", notification.getNotification().extras.getString("android.title"));
        assertNotNull(notification.getNotification().contentIntent);
        notification.getNotification().contentIntent.send();
        getInstrumentation().waitForIdleSync();
        long end = SystemClock.elapsedRealtime() + 3000;
        while (!repo.listar(teacher).stream().anyMatch(a -> a.getId() == id && a.getPublicado())
                && SystemClock.elapsedRealtime() < end) SystemClock.sleep(100);
        assertTrue(repo.listar(teacher).stream().anyMatch(a -> a.getId() == id && a.getPublicado()));
    }

    public void testWrongAccountKeepsEventPendingAndLogoutClearsNotification() {
        long id;
        try (AppDatabaseHelper helper = new AppDatabaseHelper(context)) {
            id = AvisosRepository.Companion.guardar(helper.getWritableDatabase(), student, null,
                    "Calificación de prueba", "Consulta tu calificación.", System.currentTimeMillis());
        }
        created.add(id);
        start(student, -1);
        SystemClock.sleep(1000);
        assertNull(posted(id));
        assertFalse(new AvisosRepository(context).listar(student).stream()
                .filter(a -> a.getId() == id).findFirst().get().getPublicado());
        new SessionManager(context).startSession(student);
        start(student, -1);
        assertNotNull(waitFor(id, 10000));
        new SessionManager(context).logout();
        assertNull(posted(id));
    }
}
