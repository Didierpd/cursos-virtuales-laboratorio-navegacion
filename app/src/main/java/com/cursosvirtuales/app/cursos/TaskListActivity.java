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

/**
 * HU-06 - Crear una tarea con enunciado, fecha limite y puntaje.
 *
 * Lista las tareas de un curso y permite crear nuevas o eliminarlas.
 */
public class TaskListActivity extends AppCompatActivity {

    public static final String EXTRA_CURSO_ID = "com.cursosvirtuales.app.cursos.CURSO_ID";

    private ListView lvTareas;
    private TextView tvVacio;
    private Button btnNuevaTarea;

    private TaskRepository repositorio;
    private List<CourseTask> tareas;
    private long cursoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        lvTareas = findViewById(R.id.lv_tareas);
        tvVacio = findViewById(R.id.tv_sin_tareas);
        btnNuevaTarea = findViewById(R.id.btn_nueva_tarea);

        repositorio = new TaskRepository(this);
        cursoId = getIntent().getLongExtra(EXTRA_CURSO_ID, Course.SIN_ID);
        if (!CourseAccess.allow(this, cursoId, false)) { finish(); return; }

        ((TextView) findViewById(R.id.ayuda_task)).setText(CourseAccess.allow(this, cursoId, true) ? "Toca una tarea para revisar y calificar entregas. Mantén pulsado para eliminar." : "Toca una tarea para entregarla o consultar tu calificación.");
        btnNuevaTarea.setVisibility(CourseAccess.allow(this, cursoId, true) ? View.VISIBLE : View.GONE);
        lvTareas.setOnItemClickListener((parent, view, position, id) -> {
            Intent detail = new Intent(this, TaskDetailActivity.class);
            detail.putExtra("task_id", tareas.get(position).getId());
            startActivity(detail);
        });
        btnNuevaTarea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TaskListActivity.this, CreateTaskActivity.class);
                intent.putExtra(CreateTaskActivity.EXTRA_CURSO_ID, cursoId);
                startActivity(intent);
            }
        });

        lvTareas.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int posicion, long id) {
                confirmarEliminacion(tareas.get(posicion));
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!CourseAccess.allow(this, cursoId, false)) { finish(); return; }
        cargarTareas();
    }

    private void cargarTareas() {
        tareas = repositorio.listarPorCurso(cursoId);

        java.util.List<String> textos = new java.util.ArrayList<>();
        for (CourseTask tarea : tareas) {
            textos.add(tarea.toString() + "\n" + LearningRepository.status(this, tarea));
        }
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textos);
        lvTareas.setAdapter(adaptador);

        boolean vacio = tareas.isEmpty();
        tvVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        lvTareas.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    private void confirmarEliminacion(final CourseTask tarea) {
        if (!CourseAccess.allow(this, cursoId, true)) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_eliminar_tarea)
                .setMessage(getString(R.string.confirmar_eliminar_tarea, tarea.getTitulo()))
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(R.string.btn_eliminar, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!CourseAccess.allow(TaskListActivity.this, cursoId, true)) return;
                        if (repositorio.eliminar(tarea.getId())) {
                            Toast.makeText(TaskListActivity.this,
                                    R.string.tarea_eliminada_ok, Toast.LENGTH_SHORT).show();
                            cargarTareas();
                        }
                    }
                })
                .show();
    }
}
