package com.cursosvirtuales.app.talleres.encomiendas;

import com.cursosvirtuales.app.R;
import com.cursosvirtuales.app.talleres.TallerActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Formulario de captura de encomienda + remitente + destinatario.
 *  - Botón "Registrar encomienda": guarda los datos en SQLite.
 *  - Botón "Consultar encomiendas": abre la pantalla de consulta.
 */
public class RegistroActivity extends TallerActivity {

    private EncomiendasDbHelper dbHelper;

    private TextInputLayout tilRemNombre, tilRemDocumento, tilRemTelefono, tilRemDireccion, tilRemCiudad;
    private TextInputLayout tilDesNombre, tilDesDocumento, tilDesTelefono, tilDesDireccion, tilDesCiudad;
    private TextInputLayout tilDescripcion, tilPeso, tilValor;
    private RadioGroup rgTipoEnvio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enc_activity_registro);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Registro de encomiendas");
        }

        dbHelper = new EncomiendasDbHelper(this);

        tilRemNombre = findViewById(R.id.enc_til_rem_nombre);
        tilRemDocumento = findViewById(R.id.enc_til_rem_documento);
        tilRemTelefono = findViewById(R.id.enc_til_rem_telefono);
        tilRemDireccion = findViewById(R.id.enc_til_rem_direccion);
        tilRemCiudad = findViewById(R.id.enc_til_rem_ciudad);

        tilDesNombre = findViewById(R.id.enc_til_des_nombre);
        tilDesDocumento = findViewById(R.id.enc_til_des_documento);
        tilDesTelefono = findViewById(R.id.enc_til_des_telefono);
        tilDesDireccion = findViewById(R.id.enc_til_des_direccion);
        tilDesCiudad = findViewById(R.id.enc_til_des_ciudad);

        tilDescripcion = findViewById(R.id.enc_til_enc_descripcion);
        tilPeso = findViewById(R.id.enc_til_enc_peso);
        tilValor = findViewById(R.id.enc_til_enc_valor);
        rgTipoEnvio = findViewById(R.id.enc_rg_tipo_envio);

        Button btnRegistrar = findViewById(R.id.enc_btn_registrar);
        Button btnConsultar = findViewById(R.id.enc_btn_consultar);

        // Botón 1: adición de datos en SQLite
        btnRegistrar.setOnClickListener(v -> registrarEncomienda());

        // Botón 2: consulta de los datos almacenados
        btnConsultar.setOnClickListener(v ->
                startActivity(new Intent(this, ConsultaActivity.class)));
    }

    private void registrarEncomienda() {
        if (!formularioValido()) {
            Toast.makeText(this, "Revisa los campos marcados", Toast.LENGTH_SHORT).show();
            return;
        }

        Remitente remitente = new Remitente(
                leer(tilRemNombre), leer(tilRemDocumento), leer(tilRemTelefono),
                leer(tilRemDireccion), leer(tilRemCiudad));

        Destinatario destinatario = new Destinatario(
                leer(tilDesNombre), leer(tilDesDocumento), leer(tilDesTelefono),
                leer(tilDesDireccion), leer(tilDesCiudad));

        String tipo = rgTipoEnvio.getCheckedRadioButtonId() == R.id.enc_rb_express ? "Express" : "Normal";
        String valorTxt = leer(tilValor);
        double valor = valorTxt.isEmpty() ? 0 : Double.parseDouble(valorTxt.replace(',', '.'));

        Encomienda encomienda = new Encomienda(
                generarGuia(),
                leer(tilDescripcion),
                Double.parseDouble(leer(tilPeso).replace(',', '.')),
                valor,
                tipo,
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()),
                remitente, destinatario);

        long id;
        try { id = dbHelper.insertarEncomienda(remitente, destinatario, encomienda); }
        catch (android.database.sqlite.SQLiteException ex) {
            Toast.makeText(this, "No se pudo guardar. Revisa los datos e inténtalo de nuevo.", Toast.LENGTH_LONG).show();
            return;
        }

        if (id != -1) {
            new AlertDialog.Builder(this)
                    .setTitle("Encomienda registrada")
                    .setMessage("Número de guía:\n" + encomienda.getGuia())
                    .setPositiveButton("Aceptar", null)
                    .show();
            limpiarFormulario();
        } else {
            Toast.makeText(this, "No se pudo guardar la encomienda", Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------
    // Validación
    // ------------------------------------------------------------------

    private boolean formularioValido() {
        boolean ok = true;
        TextInputLayout[] obligatorios = {
                tilRemNombre, tilRemDocumento, tilRemTelefono, tilRemDireccion, tilRemCiudad,
                tilDesNombre, tilDesDocumento, tilDesTelefono, tilDesDireccion, tilDesCiudad,
                tilDescripcion, tilPeso
        };
        for (TextInputLayout til : obligatorios) {
            if (TextUtils.isEmpty(leer(til))) {
                til.setError("Campo obligatorio");
                ok = false;
            } else {
                til.setError(null);
            }
        }

        if (!TextUtils.isEmpty(leer(tilPeso))) {
            try {
                double peso = Double.parseDouble(leer(tilPeso).replace(',', '.'));
                if (Double.isNaN(peso) || Double.isInfinite(peso) || peso <= 0) {
                    tilPeso.setError("Debe ser mayor que 0");
                    ok = false;
                }
            } catch (NumberFormatException e) {
                tilPeso.setError("Número no válido");
                ok = false;
            }
        }

        if (!leer(tilValor).isEmpty()) {
            try {
                double valor = Double.parseDouble(leer(tilValor).replace(',', '.'));
                if (Double.isNaN(valor) || Double.isInfinite(valor) || valor < 0) {
                    tilValor.setError("El valor debe ser un número mayor o igual a cero"); ok = false;
                } else tilValor.setError(null);
            } catch (NumberFormatException ex) { tilValor.setError("Número no válido"); ok = false; }
        }
        if (!leer(tilRemDocumento).isEmpty()
                && leer(tilRemDocumento).equals(leer(tilDesDocumento))) {
            tilDesDocumento.setError("Debe ser distinto al del remitente");
            ok = false;
        }
        return ok;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private String leer(TextInputLayout til) {
        return til.getEditText() == null ? "" : til.getEditText().getText().toString().trim();
    }

    /** Guía única basada en fecha y hora, p. ej. ENC-261007154412083. */
    private String generarGuia() {
        return "ENC-" + java.util.UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private void limpiarFormulario() {
        TextInputLayout[] todos = {
                tilRemNombre, tilRemDocumento, tilRemTelefono, tilRemDireccion, tilRemCiudad,
                tilDesNombre, tilDesDocumento, tilDesTelefono, tilDesDireccion, tilDesCiudad,
                tilDescripcion, tilPeso, tilValor
        };
        for (TextInputLayout til : todos) {
            if (til.getEditText() != null) til.getEditText().setText("");
            til.setError(null);
        }
        rgTipoEnvio.check(R.id.enc_rb_normal);
        tilRemNombre.requestFocus();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
