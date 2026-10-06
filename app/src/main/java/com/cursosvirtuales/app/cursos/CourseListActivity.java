package com.cursosvirtuales.app.cursos;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import com.cursosvirtuales.app.R;
import com.cursosvirtuales.app.data.AuthRepository;
import com.cursosvirtuales.app.data.SessionManager;
import com.cursosvirtuales.app.model.Role;
import com.cursosvirtuales.app.model.User;

/**
 * Listado de los cursos creados por el docente en sesion.
 *
 * Solo un docente autenticado puede ver esta pantalla; el id usado para
 * filtrar los cursos es siempre el de la sesion activa.
 */
public class CourseListActivity extends AppCompatActivity {

    private ListView lvCursos;
    private TextView tvVacio;
    private Button btnNuevo;
    private CourseRepository repositorio;
    private SessionManager sessionManager;
    private List<Course> cursos;
    private String docenteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_list);

        lvCursos = findViewById(R.id.lv_cursos);
        tvVacio = findViewById(R.id.tv_sin_cursos);
        btnNuevo = findViewById(R.id.btn_nuevo_curso);

        repositorio = new CourseRepository(this);
        sessionManager = new SessionManager(this);

        btnNuevo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CourseListActivity.this, CreateCourseActivity.class));
            }
        });

        lvCursos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int posicion, long id) {
                Intent intent = new Intent(CourseListActivity.this, CourseDetailActivity.class);
                intent.putExtra(CourseDetailActivity.EXTRA_CURSO_ID,
                        cursos.get(posicion).getId());
                startActivity(intent);
            }
        });

        lvCursos.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int posicion, long id) {
                confirmarEliminacion(cursos.get(posicion));
                return true;
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

        if (usuario == null || usuario.getRole() != Role.DOCENTE) {
            finish();
            return;
        }
        docenteId = usuario.getId();

        cargarCursos();
    }

    private void cargarCursos() {
        cursos = repositorio.listarPorDocente(docenteId);

        ArrayAdapter<Course> adaptador = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, cursos);
        lvCursos.setAdapter(adaptador);

        boolean vacio = cursos.isEmpty();
        tvVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        lvCursos.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    private void confirmarEliminacion(final Course curso) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_eliminar_curso)
                .setMessage(getString(R.string.confirmar_eliminar_curso, curso.getNombre()))
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(R.string.btn_eliminar, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (repositorio.eliminar(curso.getId())) {
                            Toast.makeText(CourseListActivity.this,
                                    R.string.curso_eliminado_ok, Toast.LENGTH_SHORT).show();
                            cargarCursos();
                        }
                    }
                })
                .show();
    }
}
