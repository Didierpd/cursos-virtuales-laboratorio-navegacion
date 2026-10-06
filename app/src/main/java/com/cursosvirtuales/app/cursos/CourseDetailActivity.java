package com.cursosvirtuales.app.cursos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cursosvirtuales.app.R;
import com.cursosvirtuales.app.data.AuthRepository;
import com.cursosvirtuales.app.data.SessionManager;
import com.cursosvirtuales.app.model.Role;
import com.cursosvirtuales.app.model.User;

/**
 * Pantalla de detalle de un curso del docente. Desde aqui se accede a
 * editar el curso, organizarlo en unidades (HU-05) y gestionar sus
 * tareas (HU-06).
 */
public class CourseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CURSO_ID = "com.cursosvirtuales.app.cursos.CURSO_ID";

    private TextView tvNombreCurso;
    private TextView tvFechasCurso;
    private Button btnEditar;
    private Button btnUnidades;
    private Button btnTareas;

    private CourseRepository repositorio;
    private SessionManager sessionManager;
    private long cursoId;
    private String docenteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        tvNombreCurso = findViewById(R.id.tv_nombre_curso_detalle);
        tvFechasCurso = findViewById(R.id.tv_fechas_curso_detalle);
        btnEditar = findViewById(R.id.btn_editar_curso_detalle);
        btnUnidades = findViewById(R.id.btn_unidades_curso);
        btnTareas = findViewById(R.id.btn_tareas_curso);

        AuthRepository.INSTANCE.init(this);
        repositorio = new CourseRepository(this);
        sessionManager = new SessionManager(this);

        cursoId = getIntent().getLongExtra(EXTRA_CURSO_ID, Course.SIN_ID);

        findViewById(R.id.btn_laboratorio).setOnClickListener(view -> {
            Intent intent = new Intent(this, LaboratoryActivity.class);
            intent.putExtra(EXTRA_CURSO_ID, cursoId);
            startActivity(intent);
        });
        btnEditar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CourseDetailActivity.this, CreateCourseActivity.class);
                intent.putExtra(CreateCourseActivity.EXTRA_CURSO_ID, cursoId);
                startActivity(intent);
            }
        });

        btnUnidades.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CourseDetailActivity.this, UnitListActivity.class);
                intent.putExtra(UnitListActivity.EXTRA_CURSO_ID, cursoId);
                startActivity(intent);
            }
        });

        btnTareas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CourseDetailActivity.this, TaskListActivity.class);
                intent.putExtra(TaskListActivity.EXTRA_CURSO_ID, cursoId);
                startActivity(intent);
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

        if (usuario == null) {
            finish();
            return;
        }
        docenteId = usuario.getId();

        cargarCurso();
    }

    private void cargarCurso() {
        Course curso = repositorio.buscarPorId(cursoId);
        if (curso == null || !CourseAccess.allow(this, cursoId, false)) {
            Toast.makeText(this, R.string.curso_no_encontrado, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        btnEditar.setVisibility(CourseAccess.allow(this, cursoId, true) ? View.VISIBLE : View.GONE);
        LearningRepository learning = new LearningRepository(this);
        androidx.appcompat.widget.SwitchCompat policy = findViewById(R.id.sw_tardias);
        TextView explanation = findViewById(R.id.tv_politica_tardias);
        policy.setOnCheckedChangeListener(null);
        policy.setChecked(learning.acceptsLate(cursoId));
        policy.setVisibility(CourseAccess.allow(this, cursoId, true) ? View.VISIBLE : View.GONE);
        explanation.setText(policy.isChecked() ? "Se aceptan entregas fuera de plazo y se registra su retraso. Los cambios de política se aplican a nuevas entregas." : "Solo se aceptan entregas hasta la fecha límite. Las entregas ya registradas se conservan al cambiar esta política.");
        policy.setOnCheckedChangeListener((button, checked) -> {
            try { learning.setAcceptsLate(cursoId, checked); }
            catch (IllegalArgumentException e) { Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show(); }
            cargarCurso();
        });
        tvNombreCurso.setText(curso.getNombre());
        tvFechasCurso.setText(Course.formatearFecha(curso.getFechaInicio())
                + "  -  " + Course.formatearFecha(curso.getFechaFin()));
    }
}
