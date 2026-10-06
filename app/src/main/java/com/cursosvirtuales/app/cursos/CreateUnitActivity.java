package com.cursosvirtuales.app.cursos;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cursosvirtuales.app.R;

/**
 * HU-05 - Crea una unidad dentro de un curso, con un nombre obligatorio
 * y un recurso/material opcional (enlace o descripcion breve).
 */
public class CreateUnitActivity extends AppCompatActivity {

    public static final String EXTRA_CURSO_ID = "com.cursosvirtuales.app.cursos.CURSO_ID";

    private EditText etNombre;
    private EditText etRecurso;
    private Button btnGuardar;
    private TextView tvError;

    private UnitRepository repositorio;
    private long cursoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_unit);

        etNombre = findViewById(R.id.et_nombre_unidad);
        etRecurso = findViewById(R.id.et_recurso_unidad);
        btnGuardar = findViewById(R.id.btn_guardar_unidad);
        tvError = findViewById(R.id.tv_error_unidad);

        repositorio = new UnitRepository(this);
        cursoId = getIntent().getLongExtra(EXTRA_CURSO_ID, Course.SIN_ID);
        if (!CourseAccess.allow(this, cursoId, true)) { finish(); return; }

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarUnidad();
            }
        });
    }

    private void guardarUnidad() {
        if (!CourseAccess.allow(this, cursoId, true)) { finish(); return; }
        tvError.setVisibility(View.GONE);

        String nombre = etNombre.getText().toString().trim();
        String recurso = etRecurso.getText().toString().trim();

        if (nombre.isEmpty()) {
            tvError.setText(R.string.error_nombre_unidad_obligatorio);
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        long id = repositorio.insertar(cursoId, nombre, recurso.isEmpty() ? null : recurso);

        if (id != -1L) {
            Toast.makeText(this, R.string.unidad_guardada_ok, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            tvError.setText(R.string.error_guardar_unidad);
            tvError.setVisibility(View.VISIBLE);
        }
    }
}
