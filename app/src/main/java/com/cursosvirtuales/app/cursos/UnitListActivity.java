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
 * HU-05 - Organizar el curso en unidades o modulos.
 *
 * Lista las unidades de un curso en el orden en que se mostrarian al
 * estudiante y permite crear, reordenar y eliminar unidades.
 */
public class UnitListActivity extends AppCompatActivity {

    public static final String EXTRA_CURSO_ID = "com.cursosvirtuales.app.cursos.CURSO_ID";

    private ListView lvUnidades;
    private TextView tvVacio;
    private Button btnNuevaUnidad;
    private Button btnSubir;
    private Button btnBajar;

    private UnitRepository repositorio;
    private List<CourseUnit> unidades;
    private long cursoId;
    private int posicionSeleccionada = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unit_list);

        lvUnidades = findViewById(R.id.lv_unidades);
        tvVacio = findViewById(R.id.tv_sin_unidades);
        btnNuevaUnidad = findViewById(R.id.btn_nueva_unidad);
        btnSubir = findViewById(R.id.btn_subir_unidad);
        btnBajar = findViewById(R.id.btn_bajar_unidad);

        repositorio = new UnitRepository(this);
        cursoId = getIntent().getLongExtra(EXTRA_CURSO_ID, Course.SIN_ID);
        if (!CourseAccess.allow(this, cursoId, false)) { finish(); return; }

        boolean docente = CourseAccess.allow(this, cursoId, true);
        ((TextView) findViewById(R.id.ayuda_unit)).setText(docente ? "Selecciona una unidad para moverla. Mantén pulsado para eliminar." : "Toca una unidad para consultar su material.");
        btnNuevaUnidad.setVisibility(docente ? View.VISIBLE : View.GONE);
        btnSubir.setVisibility(docente ? View.VISIBLE : View.GONE);
        btnBajar.setVisibility(docente ? View.VISIBLE : View.GONE);

        btnNuevaUnidad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UnitListActivity.this, CreateUnitActivity.class);
                intent.putExtra(CreateUnitActivity.EXTRA_CURSO_ID, cursoId);
                startActivity(intent);
            }
        });

        lvUnidades.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int posicion, long id) {
                if (!CourseAccess.allow(UnitListActivity.this, cursoId, true)) {
                    CourseUnit unit = unidades.get(posicion);
                    TextView material = new TextView(UnitListActivity.this);
                    material.setPadding(32, 24, 32, 24);
                    material.setTextSize(18);
                    material.setAutoLinkMask(android.text.util.Linkify.WEB_URLS);
                    material.setText(unit.getRecurso() == null ? "Aún no hay material para esta unidad." : unit.getRecurso());
                    material.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
                    new AlertDialog.Builder(UnitListActivity.this).setTitle(unit.getNombre()).setView(material).setPositiveButton("Cerrar", null).show();
                    return;
                }
                posicionSeleccionada = posicion;
                actualizarBotonesOrden();
            }
        });

        lvUnidades.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int posicion, long id) {
                confirmarEliminacion(unidades.get(posicion));
                return true;
            }
        });

        btnSubir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moverSeleccionada(true);
            }
        });

        btnBajar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moverSeleccionada(false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!CourseAccess.allow(this, cursoId, false)) { finish(); return; }
        cargarUnidades();
    }

    private void cargarUnidades() {
        unidades = repositorio.listarPorCurso(cursoId);
        posicionSeleccionada = -1;

        ArrayAdapter<CourseUnit> adaptador = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, unidades);
        lvUnidades.setAdapter(adaptador);

        boolean vacio = unidades.isEmpty();
        tvVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        lvUnidades.setVisibility(vacio ? View.GONE : View.VISIBLE);
        actualizarBotonesOrden();
    }

    private void actualizarBotonesOrden() {
        boolean haySeleccion = posicionSeleccionada >= 0 && posicionSeleccionada < unidades.size();
        btnSubir.setEnabled(haySeleccion && posicionSeleccionada > 0);
        btnBajar.setEnabled(haySeleccion && posicionSeleccionada < unidades.size() - 1);
    }

    private void moverSeleccionada(boolean haciaArriba) {
        if (!CourseAccess.allow(this, cursoId, true)) return;
        if (posicionSeleccionada < 0 || posicionSeleccionada >= unidades.size()) {
            return;
        }
        CourseUnit unidad = unidades.get(posicionSeleccionada);
        if (repositorio.mover(unidad, haciaArriba)) {
            cargarUnidades();
        }
    }

    private void confirmarEliminacion(final CourseUnit unidad) {
        if (!CourseAccess.allow(this, cursoId, true)) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_eliminar_unidad)
                .setMessage(getString(R.string.confirmar_eliminar_unidad, unidad.getNombre()))
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(R.string.btn_eliminar, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!CourseAccess.allow(UnitListActivity.this, cursoId, true)) return;
                        if (repositorio.eliminar(unidad.getId())) {
                            Toast.makeText(UnitListActivity.this,
                                    R.string.unidad_eliminada_ok, Toast.LENGTH_SHORT).show();
                            cargarUnidades();
                        }
                    }
                })
                .show();
    }
}
