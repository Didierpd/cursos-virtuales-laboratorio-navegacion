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
import com.cursosvirtuales.app.data.AuthRepository;
import com.cursosvirtuales.app.data.SessionManager;
import com.cursosvirtuales.app.model.Role;
import com.cursosvirtuales.app.model.User;

/**
 * HU-03 - Crear un curso con su nombre, descripcion y fechas.
 *
 * La misma pantalla atiende la creacion y la edicion: si se recibe
 * EXTRA_CURSO_ID el formulario se precarga con los datos del curso.
 * Solo el docente autenticado en la sesion activa puede crear o editar
 * sus propios cursos (nunca se recibe el id del docente por fuera).
 */
public class CreateCourseActivity extends AppCompatActivity {

    public static final String EXTRA_CURSO_ID = "com.cursosvirtuales.app.cursos.CURSO_ID";

    private EditText etNombre;
    private EditText etDescripcion;
    private Button btnFechaInicio;
    private Button btnFechaFin;
    private Button btnGuardar;
    private TextView tvError;

    private CourseRepository repositorio;
    private SessionManager sessionManager;

    private Long fechaInicio = null;
    private Long fechaFin = null;
    private long cursoId = Course.SIN_ID;
    private String docenteId;
    private boolean formularioCargado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_course);

        etNombre = findViewById(R.id.et_nombre_curso);
        etDescripcion = findViewById(R.id.et_descripcion_curso);
        btnFechaInicio = findViewById(R.id.btn_fecha_inicio);
        btnFechaFin = findViewById(R.id.btn_fecha_fin);
        btnGuardar = findViewById(R.id.btn_guardar_curso);
        tvError = findViewById(R.id.tv_error_curso);

        repositorio = new CourseRepository(this);
        sessionManager = new SessionManager(this);

        btnFechaInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarSelectorFecha(true);
            }
        });

        btnFechaFin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarSelectorFecha(false);
            }
        });

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarCurso();
            }
        });

        cursoId = getIntent().getLongExtra(EXTRA_CURSO_ID, Course.SIN_ID);
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

        if (!formularioCargado) {
            if (cursoId != Course.SIN_ID) {
                precargarCurso(cursoId);
            }
            formularioCargado = true;
        }
    }

    /** Carga en el formulario los datos de un curso existente (modo edicion). */
    private void precargarCurso(long id) {
        Course curso = repositorio.buscarPorId(id);
        if (curso == null || !curso.getDocenteId().equals(docenteId)) {
            Toast.makeText(this, R.string.curso_no_encontrado, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setTitle(R.string.titulo_editar_curso);
        etNombre.setText(curso.getNombre());
        etDescripcion.setText(curso.getDescripcion());
        fechaInicio = curso.getFechaInicio();
        fechaFin = curso.getFechaFin();
        btnFechaInicio.setText(Course.formatearFecha(fechaInicio));
        btnFechaFin.setText(Course.formatearFecha(fechaFin));
        btnGuardar.setText(R.string.btn_guardar_cambios);
    }

    /** Abre el calendario de Android para elegir la fecha de inicio o de fin. */
    private void mostrarSelectorFecha(final boolean esInicio) {
        Calendar calendario = Calendar.getInstance();
        Long actual = esInicio ? fechaInicio : fechaFin;
        if (actual != null) {
            calendario.setTimeInMillis(actual);
        }

        DatePickerDialog dialogo = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int anio, int mes, int dia) {
                        Calendar elegida = Calendar.getInstance();
                        elegida.set(anio, mes, dia, 0, 0, 0);
                        elegida.set(Calendar.MILLISECOND, 0);

                        if (esInicio) {
                            fechaInicio = elegida.getTimeInMillis();
                            btnFechaInicio.setText(Course.formatearFecha(fechaInicio));
                        } else {
                            fechaFin = elegida.getTimeInMillis();
                            btnFechaFin.setText(Course.formatearFecha(fechaFin));
                        }
                        ocultarError();
                    }
                },
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH));

        dialogo.show();
    }

    /** Valida el formulario y guarda el curso en la base de datos. */
    private void guardarCurso() {
        ocultarError();

        String nombre = etNombre.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();

        CourseValidator.Resultado resultado =
                CourseValidator.validar(nombre, descripcion, fechaInicio, fechaFin);

        if (resultado == CourseValidator.Resultado.OK
                && repositorio.existeNombre(nombre, docenteId, cursoId)) {
            resultado = CourseValidator.Resultado.NOMBRE_DUPLICADO;
        }

        if (resultado != CourseValidator.Resultado.OK) {
            mostrarError(mensajeDeError(resultado));
            return;
        }

        Course curso = new Course(
                cursoId, nombre, descripcion, fechaInicio, fechaFin, docenteId);

        boolean exito;
        if (cursoId == Course.SIN_ID) {
            long nuevoId = repositorio.insertar(curso);
            exito = nuevoId != -1L;
        } else {
            exito = repositorio.actualizar(curso);
        }

        if (exito) {
            Toast.makeText(this, R.string.curso_guardado_ok, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            mostrarError(getString(R.string.error_guardar_curso));
        }
    }

    private String mensajeDeError(CourseValidator.Resultado resultado) {
        switch (resultado) {
            case NOMBRE_VACIO:
                return getString(R.string.error_nombre_obligatorio);
            case NOMBRE_MUY_CORTO:
                return getString(R.string.error_nombre_corto, CourseValidator.LARGO_MINIMO_NOMBRE);
            case DESCRIPCION_VACIA:
                return getString(R.string.error_descripcion_obligatoria);
            case FECHAS_INCOMPLETAS:
                return getString(R.string.error_fechas_obligatorias);
            case FIN_ANTES_DE_INICIO:
                return getString(R.string.error_fecha_fin);
            case NOMBRE_DUPLICADO:
                return getString(R.string.error_nombre_duplicado);
            default:
                return getString(R.string.error_guardar_curso);
        }
    }

    private void mostrarError(String mensaje) {
        tvError.setText(mensaje);
        tvError.setVisibility(View.VISIBLE);
    }

    private void ocultarError() {
        tvError.setVisibility(View.GONE);
    }
}
