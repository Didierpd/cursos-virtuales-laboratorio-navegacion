package com.cursosvirtuales.app.cursos;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import com.cursosvirtuales.app.R;
import com.cursosvirtuales.app.data.AuthRepository;
import com.cursosvirtuales.app.data.SessionManager;
import com.cursosvirtuales.app.model.Role;
import com.cursosvirtuales.app.model.User;

/**
 * HU-04 - Inscribirme en un curso disponible.
 *
 * Muestra todos los cursos de la plataforma (de cualquier docente). Al tocar
 * uno, pide confirmacion y lo inscribe; si ya estaba inscrito, lo indica.
 * Solo un estudiante autenticado puede ver esta pantalla.
 */
public class AvailableCoursesActivity extends AppCompatActivity {

    private ListView lvCursos;
    private TextView tvVacio;
    private CourseRepository cursoRepositorio;
    private EnrollmentRepository inscripcionRepositorio;
    private SessionManager sessionManager;

    private List<Course> cursos = new ArrayList<>();
    private String estudianteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_courses);

        lvCursos = findViewById(R.id.lv_cursos_disponibles);
        tvVacio = findViewById(R.id.tv_sin_cursos_disponibles);

        cursoRepositorio = new CourseRepository(this);
        inscripcionRepositorio = new EnrollmentRepository(this);
        sessionManager = new SessionManager(this);

        lvCursos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int posicion, long id) {
                confirmarInscripcion(cursos.get(posicion));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!sessionManager.isSessionValid()) {
            finish();
            return;
        }
        sessionManager.touch();

        String userId = sessionManager.currentUserId();
        User usuario = userId != null ? AuthRepository.INSTANCE.findById(userId) : null;

        if (usuario == null || usuario.getRole() != Role.ESTUDIANTE) {
            finish();
            return;
        }
        estudianteId = usuario.getId();

        cargarCursos();
    }

    private void cargarCursos() {
        cursos = cursoRepositorio.listarTodos();

        List<String> textos = new ArrayList<>();
        for (Course curso : cursos) {
            boolean inscrito = inscripcionRepositorio.estaInscrito(curso.getId(), estudianteId);
            String etiqueta = inscrito ? "  " + getString(R.string.etiqueta_ya_inscrito) : "";
            textos.add(curso.toString() + etiqueta);
        }

        ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, textos);
        lvCursos.setAdapter(adaptador);

        boolean vacio = cursos.isEmpty();
        tvVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        lvCursos.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    private void confirmarInscripcion(final Course curso) {
        if (inscripcionRepositorio.estaInscrito(curso.getId(), estudianteId)) {
            android.content.Intent intent = new android.content.Intent(this, CourseDetailActivity.class);
            intent.putExtra(CourseDetailActivity.EXTRA_CURSO_ID, curso.getId());
            startActivity(intent);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_inscribirme)
                .setMessage(getString(R.string.confirmar_inscripcion, curso.getNombre()))
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(R.string.btn_inscribirme, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean exito = inscripcionRepositorio.inscribir(curso.getId(), estudianteId);
                        if (exito) {
                            Toast.makeText(AvailableCoursesActivity.this,
                                    R.string.inscripcion_ok, Toast.LENGTH_SHORT).show();
                            cargarCursos();
                        } else {
                            Toast.makeText(AvailableCoursesActivity.this,
                                    R.string.mensaje_ya_inscrito, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }
}
