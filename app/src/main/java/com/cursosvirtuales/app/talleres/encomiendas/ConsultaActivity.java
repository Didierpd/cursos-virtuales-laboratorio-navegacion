package com.cursosvirtuales.app.talleres.encomiendas;

import com.cursosvirtuales.app.R;
import com.cursosvirtuales.app.talleres.TallerActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/**
 * Consulta de los datos almacenados en SQLite.
 *  - Al abrir, lista todas las encomiendas.
 *  - "Buscar" filtra por número de guía o documento (remitente o destinatario).
 *  - Tocar una encomienda muestra su detalle completo.
 */
public class ConsultaActivity extends TallerActivity {

    private EncomiendasDbHelper dbHelper;
    private EditText etBuscar;
    private ListView lvEncomiendas;
    private TextView tvResumen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enc_activity_consulta);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Consulta de encomiendas");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new EncomiendasDbHelper(this);
        etBuscar = findViewById(R.id.enc_et_buscar);
        lvEncomiendas = findViewById(R.id.enc_lv_encomiendas);
        tvResumen = findViewById(R.id.enc_tv_resumen);
        lvEncomiendas.setEmptyView(findViewById(R.id.enc_tv_vacio));

        Button btnBuscar = findViewById(R.id.enc_btn_buscar);
        Button btnVerTodas = findViewById(R.id.enc_btn_ver_todas);

        btnBuscar.setOnClickListener(v -> {
            String texto = etBuscar.getText().toString().trim();
            if (TextUtils.isEmpty(texto)) {
                etBuscar.setError("Escribe una guía o un documento");
                return;
            }
            mostrar(dbHelper.buscarEncomiendas(texto), "Resultados para \"" + texto + "\"");
        });

        btnVerTodas.setOnClickListener(v -> {
            etBuscar.setText("");
            cargarTodas();
        });

        cargarTodas();
    }

    private void cargarTodas() {
        mostrar(dbHelper.consultarEncomiendas(), "Todas las encomiendas");
    }

    private void mostrar(List<Encomienda> encomiendas, String titulo) {
        tvResumen.setText(titulo + ": " + encomiendas.size());

        ArrayAdapter<Encomienda> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, encomiendas);
        lvEncomiendas.setAdapter(adapter);

        lvEncomiendas.setOnItemClickListener((parent, view, position, id) -> {
            Encomienda e = encomiendas.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Detalle de la encomienda")
                    .setMessage(e.detalle())
                    .setPositiveButton("Cerrar", null)
                    .show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
