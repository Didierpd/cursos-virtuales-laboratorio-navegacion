package com.cursosvirtuales.app.cursos;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

import com.cursosvirtuales.app.R;

/**
 * HU-06 - Crear una tarea con enunciado, fecha limite y puntaje.
 */
public class CreateTaskActivity extends AppCompatActivity {

    public static final String EXTRA_CURSO_ID = "com.cursosvirtuales.app.cursos.CURSO_ID";

    private EditText etTitulo;
    private EditText etEnunciado;
    private EditText etPuntaje;
    private Button btnFechaLimite;
    private Button btnGuardar;
    private TextView tvError;

    private TaskRepository repositorio;
    private long cursoId;
    private Long fechaLimite = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        etTitulo = findViewById(R.id.et_titulo_tarea);
        etEnunciado = findViewById(R.id.et_enunciado_tarea);
        etPuntaje = findViewById(R.id.et_puntaje_tarea);
        btnFechaLimite = findViewById(R.id.btn_fecha_limite_tarea);
        btnGuardar = findViewById(R.id.btn_guardar_tarea);
        tvError = findViewById(R.id.tv_error_tarea);

        repositorio = new TaskRepository(this);
        cursoId = getIntent().getLongExtra(EXTRA_CURSO_ID, Course.SIN_ID);
        if (!CourseAccess.allow(this, cursoId, true)) { finish(); return; }

        if (savedInstanceState != null && savedInstanceState.containsKey("fecha")) {
            fechaLimite = savedInstanceState.getLong("fecha");
            btnFechaLimite.setText(CourseTask.formatearFecha(fechaLimite));
        }
        btnFechaLimite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarSelectorFecha();
            }
        });

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarTarea();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        if (fechaLimite != null) state.putLong("fecha", fechaLimite);
        super.onSaveInstanceState(state);
    }

    private void mostrarSelectorFecha() {
        Calendar calendario = Calendar.getInstance();
        if (fechaLimite != null) {
            calendario.setTimeInMillis(fechaLimite);
        }

        DatePickerDialog dialogo = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int anio, int mes, int dia) {
                        Calendar elegida = Calendar.getInstance();
                        elegida.set(anio, mes, dia, 23, 59, 59);
                        elegida.set(Calendar.MILLISECOND, 999);
                        fechaLimite = elegida.getTimeInMillis();
                        btnFechaLimite.setText(CourseTask.formatearFecha(fechaLimite));
                        tvError.setVisibility(View.GONE);
                    }
                },
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH));

        dialogo.show();
    }

    private void guardarTarea() {
        if (!CourseAccess.allow(this, cursoId, true)) { finish(); return; }
        tvError.setVisibility(View.GONE);

        String titulo = etTitulo.getText().toString().trim();
        String enunciado = etEnunciado.getText().toString().trim();
        String puntajeTexto = etPuntaje.getText().toString().trim();

        TaskValidator.Resultado resultado =
                TaskValidator.validar(titulo, enunciado, fechaLimite, puntajeTexto);

        if (resultado != TaskValidator.Resultado.OK) {
            mostrarError(mensajeDeError(resultado));
            return;
        }

        int puntaje = Integer.parseInt(puntajeTexto);
        CourseTask tarea = new CourseTask(
                CourseTask.SIN_ID, cursoId, titulo, enunciado, fechaLimite, puntaje);

        long id = repositorio.insertar(tarea);

        if (id != -1L) {
            Toast.makeText(this, R.string.tarea_guardada_ok, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            mostrarError(getString(R.string.error_guardar_tarea));
        }
    }

    private String mensajeDeError(TaskValidator.Resultado resultado) {
        switch (resultado) {
            case TITULO_VACIO:
                return getString(R.string.error_titulo_tarea_obligatorio);
            case ENUNCIADO_VACIO:
                return getString(R.string.error_enunciado_tarea_obligatorio);
            case FECHA_LIMITE_VACIA:
                return getString(R.string.error_fecha_limite_obligatoria);
            case PUNTAJE_INVALIDO:
                return getString(R.string.error_puntaje_invalido, TaskValidator.PUNTAJE_MAXIMO);
            default:
                return getString(R.string.error_guardar_tarea);
        }
    }

    private void mostrarError(String mensaje) {
        tvError.setText(mensaje);
        tvError.setVisibility(View.VISIBLE);
    }
}
