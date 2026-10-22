package com.cursosvirtuales.app.talleres.encomiendas;

import com.cursosvirtuales.app.R;
import com.cursosvirtuales.app.talleres.TallerActivity;

/** Persona que envía la encomienda. */
public class Remitente extends Persona {
    public Remitente(String nombre, String documento, String telefono,
                     String direccion, String ciudad) {
        super(nombre, documento, telefono, direccion, ciudad);
    }
}
