package com.cursosvirtuales.app;

import android.test.AndroidTestCase;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import com.cursosvirtuales.app.cursos.*;
import com.cursosvirtuales.app.data.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Runs against a separate database and preferences; never clears the user's data. */
public class LearningFlowTest extends AndroidTestCase {
    private Context isolated;
    private long courseId, taskId;
    private LearningRepository repo;
    private final String teacher = "u-docente-1", student = "u-estudiante-1";
    @Override protected void setUp() throws Exception {
        super.setUp();
        isolated = new ContextWrapper(getContext()) {
            @Override public Context getApplicationContext() { return this; }
            @Override public File getDatabasePath(String name) { return super.getDatabasePath("test_learning_" + name); }
            @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
                return super.openOrCreateDatabase("test_learning_" + name, mode, factory);
            }
            @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, android.database.DatabaseErrorHandler handler) {
                return super.openOrCreateDatabase("test_learning_" + name, mode, factory, handler);
            }
            @Override public android.content.SharedPreferences getSharedPreferences(String name, int mode) { return super.getSharedPreferences("test_learning_" + name,mode); }
            @Override public File getFilesDir() { File dir = new File(super.getFilesDir(),"test_learning"); dir.mkdirs(); return dir; }
        };
        try (AppDatabaseHelper clean = new AppDatabaseHelper(isolated)) {
            clean.getWritableDatabase().delete("CURSO",null,null);
        }
        AuthRepository.INSTANCE.init(isolated);
        new SessionManager(isolated).startSession(teacher);
        long now = System.currentTimeMillis();
        courseId = new CourseRepository(isolated).insertar(new Course(-1,"Curso de prueba","Descripción",now,now+86400000,teacher));
        taskId = new TaskRepository(isolated).insertar(new CourseTask(-1,courseId,"Tarea","Enunciado",now+3600000,100));
        repo = new LearningRepository(isolated);
    }
    private void login(String id) { new SessionManager(isolated).startSession(id); }
    private void enroll() { new EnrollmentRepository(isolated).inscribir(courseId,student); login(student); }
    private Uri file(String name) throws Exception {
        File file = new File(isolated.getCacheDir(),name);
        try(FileOutputStream stream = new FileOutputStream(file)) { stream.write("Contenido de prueba".getBytes()); }
        return Uri.fromFile(file);
    }
    private void rejected(Runnable action) {
        try { action.run(); fail("Debió rechazarse la operación"); } catch (IllegalArgumentException | IllegalStateException expected) { }
    }
    public void testCompleteDeliveryAndEditableGrade() throws Exception {
        enroll();
        repo.submit(taskId,Arrays.asList(file("evidencia1.txt"),file("evidencia2.txt")));
        LearningRepository.Submission submission = repo.submissions(repo.task(taskId)).get(0);
        assertEquals(2,submission.getFiles().size());
        assertTrue(submission.getDate() > 0);
        rejected(() -> repo.submit(taskId,Collections.singletonList(Uri.EMPTY)));
        rejected(() -> repo.grade(taskId,submission.getId(),80,"No autorizado"));
        login(teacher);
        rejected(() -> repo.grade(taskId,submission.getId(),101,"Fuera de rango"));
        rejected(() -> repo.grade(taskId,submission.getId(),Double.NaN,"Inválida"));
        rejected(() -> repo.grade(taskId,submission.getId(),90," "));
        repo.grade(taskId,submission.getId(),80,"Buen trabajo");
        repo.grade(taskId,submission.getId(),95,"Corrección revisada");
        login(student);
        LearningRepository.Submission saved = new LearningRepository(isolated).submissions(repo.task(taskId)).get(0);
        assertEquals(95.0,saved.getGrade(),0.001);
        assertEquals("Corrección revisada",saved.getFeedback());
        assertTrue(new File(isolated.getFilesDir(),"entregas/"+saved.getFiles().get(0).getPath()).exists());
    }
    public void testAccessDeadlineAndMissingFiles() throws Exception {
        login(student);
        assertFalse(CourseAccess.allow(isolated,courseId,false));
        rejected(() -> repo.submissions(repo.task(taskId)));
        enroll();
        assertTrue(CourseAccess.allow(isolated,courseId,false));
        assertFalse(CourseAccess.allow(isolated,courseId,true));
        rejected(() -> repo.submit(taskId,Collections.emptyList()));
        long expired = new TaskRepository(isolated).insertar(new CourseTask(-1,courseId,"Vencida","Enunciado",System.currentTimeMillis()-1000,50));
        Uri uri = file("vencida.txt");
        rejected(() -> repo.submit(expired,Collections.singletonList(uri)));
        assertTrue(LearningRepository.status(isolated,repo.task(expired)).contains("Vencida"));
        long other = new CourseRepository(isolated).insertar(new Course(-1,"Ajeno","Descripción",0,99999999,"otro-docente"));
        login(teacher);
        assertFalse(CourseAccess.allow(isolated,other,true));
        new SessionManager(isolated).logout();
        assertFalse(CourseAccess.allow(isolated,courseId,true));
    }
    public void testUnitOrderingAfterDeleteAndInsert() {
        UnitRepository units = new UnitRepository(isolated);
        long a=units.insertar(courseId,"A","Material A");
        units.insertar(courseId,"B","Material B");
        units.eliminar(a);
        long c=units.insertar(courseId,"C","Material C");
        List<CourseUnit> rows=units.listarPorCurso(courseId);
        assertTrue(rows.get(0).getOrden()!=rows.get(1).getOrden());
        assertTrue(units.mover(rows.get(1),true));
        assertEquals(c,units.listarPorCurso(courseId).get(0).getId());
    }
    public void testUpgradeKeepsExistingData() {
        AppDatabaseHelper helper = new AppDatabaseHelper(isolated);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("DROP TABLE ADJUNTO"); db.execSQL("DROP TABLE ENTREGA");
        db.setVersion(3); helper.close();
        helper = new AppDatabaseHelper(isolated);
        assertEquals(6,helper.getReadableDatabase().getVersion());
        helper.close();
        assertNotNull(new CourseRepository(isolated).buscarPorId(courseId));
        assertNotNull(new LearningRepository(isolated).task(taskId));
        assertTrue(new LearningRepository(isolated).submissions(repo.task(taskId)).isEmpty());
    }
    private void otherStudent() {
        try (AppDatabaseHelper helper = new AppDatabaseHelper(isolated)) {
            helper.getWritableDatabase().execSQL("INSERT OR IGNORE INTO USUARIO (_id,USERNAME,PASSWORD_HASH,ROL,NOMBRE_COMPLETO,EMAIL) SELECT 'test-student-2','test_student_2',PASSWORD_HASH,'ESTUDIANTE','Otro estudiante','otro@example.test' FROM USUARIO WHERE _id='u-estudiante-1'");
        }
    }

    public void testLatePolicyAcceptsRejectsAndKeepsHistory() throws Exception {
        assertFalse(repo.acceptsLate(courseId));
        long expired = new TaskRepository(isolated).insertar(new CourseTask(-1,courseId,"Vencida","Guía",System.currentTimeMillis()-3600000,100));
        enroll();
        Uri attachment = file("late.txt");
        rejected(() -> repo.submit(expired,Collections.singletonList(attachment)));
        rejected(() -> repo.setAcceptsLate(courseId,true));
        login(teacher);
        repo.setAcceptsLate(courseId,true);
        assertTrue(new LearningRepository(isolated).acceptsLate(courseId));
        login(student);
        repo.submit(expired,Collections.singletonList(attachment));
        LearningRepository.Submission delivery = repo.submissions(repo.task(expired)).get(0);
        assertTrue(delivery.getDifferenceMillis() >= 3600000);
        assertTrue(repo.timing(delivery).contains("Fuera de plazo"));
        login(teacher);
        repo.setAcceptsLate(courseId,false);
        assertEquals(1,repo.submissions(repo.task(expired)).size());
        otherStudent();
        new EnrollmentRepository(isolated).inscribir(courseId,"test-student-2");
        login("test-student-2");
        rejected(() -> repo.submit(expired,Collections.singletonList(attachment)));
        assertTrue(repo.submissions(repo.task(expired)).isEmpty());
    }

    public void testGradebookTotalsFeedbackAndPrivacy() throws Exception {
        new TaskRepository(isolated).insertar(new CourseTask(-1,courseId,"Pendiente","Guía",System.currentTimeMillis()+3600000,50));
        enroll();
        repo.submit(taskId,Collections.singletonList(file("notas.txt")));
        long first = repo.submissions(repo.task(taskId)).get(0).getId();
        login(teacher); repo.grade(taskId,first,80,"Comentario privado de Luis");
        otherStudent();
        new EnrollmentRepository(isolated).inscribir(courseId,"test-student-2");
        login("test-student-2");
        repo.submit(taskId,Collections.singletonList(file("otro.txt")));
        long second = repo.submissions(repo.task(taskId)).get(0).getId();
        login(teacher); repo.grade(taskId,second,40,"Comentario del otro estudiante");
        rejected(() -> new GradebookRepository(isolated).mine());
        new CourseRepository(isolated).insertar(new Course(-1,"Curso no inscrito","Guía",0,99999999,teacher));
        login(student);
        List<GradebookRepository.CourseGrades> courses = new GradebookRepository(isolated).mine();
        assertEquals(1,courses.size());
        GradebookRepository.CourseGrades report = courses.get(0);
        assertEquals(80.0,report.getEarned(),0.001);
        assertEquals(150,report.getAvailable());
        assertEquals(100,report.getGradedMaximum());
        assertEquals(1,report.getGradedCount());
        assertEquals("Comentario privado de Luis",report.getRows().stream().filter(r -> r.getTaskId()==taskId).findFirst().get().getFeedback());
        login(teacher); repo.grade(taskId,first,90,"Comentario actualizado");
        login(student); assertEquals(90.0,new GradebookRepository(isolated).mine().get(0).getEarned(),0.001);
        login("test-student-2"); assertEquals(40.0,new GradebookRepository(isolated).mine().get(0).getEarned(),0.001);
        new SessionManager(isolated).logout();
        rejected(() -> new GradebookRepository(isolated).mine());
    }

    public void testLaboratoryResourcesPersistenceAndPermissions() throws Exception {
        LaboratoryRepository labs = new LaboratoryRepository(isolated);
        rejected(() -> labs.create(courseId," ","Objetivo","Guía",Collections.emptyList()));
        long id = labs.create(courseId,"Práctica 1","Aprender medición","1. Preparar. 2. Medir.",Arrays.asList(file("guia.txt"),file("datos.csv")));
        enroll();
        LaboratoryRepository.Laboratory lab = new LaboratoryRepository(isolated).list(courseId).get(0);
        assertEquals(id,lab.getId());
        assertEquals("Aprender medición",lab.getObjective());
        assertEquals(2,lab.getResources().size());
        assertTrue(new File(isolated.getFilesDir(),"laboratorios/"+lab.getResources().get(0).getPath()).exists());
        rejected(() -> labs.create(courseId,"Ajena","Objetivo","Guía",Collections.emptyList()));
        otherStudent(); login("test-student-2");
        rejected(() -> labs.list(courseId));
        login(teacher);
        long other = new CourseRepository(isolated).insertar(new Course(-1,"Ajeno","Guía",0,99999999,"otro-docente"));
        rejected(() -> labs.create(other,"Ajena","Objetivo","Guía",Collections.emptyList()));
    }

    public void testLaboratoryUnreadableResourceRollsBack() throws Exception {
        File folder = new File(isolated.getFilesDir(),"laboratorios");
        int before = folder.list() == null ? 0 : folder.list().length;
        LaboratoryRepository labs = new LaboratoryRepository(isolated);
        try {
            labs.create(courseId,"Práctica","Objetivo","Guía",Arrays.asList(file("bueno.txt"),Uri.fromFile(new File(isolated.getCacheDir(),"missing-resource-never-created"))));
            fail("Debió rechazar el recurso ilegible");
        } catch (Exception expected) { assertNotNull(expected); }
        assertTrue(labs.list(courseId).isEmpty());
        assertEquals(before,folder.list() == null ? 0 : folder.list().length);
    }

    public void testRealVersion4MigrationPreservesGradeAndMeasuresDelay() {
        String name = "migration_" + System.nanoTime() + ".db";
        try (SQLiteDatabase db = isolated.openOrCreateDatabase(name,Context.MODE_PRIVATE,null)) {
            db.execSQL("CREATE TABLE CURSO (_id INTEGER PRIMARY KEY, NOMBRE TEXT, DESCRIPCION TEXT, FECHA_INICIO INTEGER, FECHA_FIN INTEGER, DOCENTE_ID TEXT)");
            db.execSQL("CREATE TABLE TAREA (_id INTEGER PRIMARY KEY, CURSO_ID INTEGER, TITULO TEXT, ENUNCIADO TEXT, FECHA_LIMITE INTEGER, PUNTAJE INTEGER)");
            db.execSQL("CREATE TABLE ENTREGA (_id INTEGER PRIMARY KEY, TAREA_ID INTEGER, ESTUDIANTE_ID TEXT, FECHA INTEGER, NOTA REAL, COMENTARIO TEXT)");
            db.execSQL("INSERT INTO CURSO VALUES(1,'Anterior','Guía',0,10000,'docente')");
            db.execSQL("INSERT INTO TAREA VALUES(1,1,'Tarea','Guía',1000,100)");
            db.execSQL("INSERT INTO ENTREGA VALUES(1,1,'estudiante',2000,85,'Conservar comentario')");
            try(AppDatabaseHelper helper = new AppDatabaseHelper(isolated)) { helper.onUpgrade(db,4,6); }
            try(android.database.Cursor c = db.rawQuery("SELECT NOTA,COMENTARIO,DIFERENCIA_MS FROM ENTREGA",null)) {
                assertTrue(c.moveToFirst()); assertEquals(85.0,c.getDouble(0),0.001);
                assertEquals("Conservar comentario",c.getString(1)); assertEquals(1000L,c.getLong(2));
            }
            try(android.database.Cursor c = db.rawQuery("SELECT ACEPTA_TARDIAS FROM CURSO",null)) { assertTrue(c.moveToFirst()); assertEquals(0,c.getInt(0)); }
            try(android.database.Cursor c = db.rawQuery("SELECT COUNT(*) FROM LABORATORIO",null)) { assertTrue(c.moveToFirst()); assertEquals(0,c.getInt(0)); }
        } finally { isolated.deleteDatabase("test_learning_"+name); }
    }

}
