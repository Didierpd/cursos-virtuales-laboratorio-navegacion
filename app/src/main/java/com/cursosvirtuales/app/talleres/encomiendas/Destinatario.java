package com.cursosvirtuales.app.talleres.encomiendas;

import com.cursosvirtuales.app.R;
import com.cursosvirtuales.app.talleres.TallerActivity;

/** Persona que recibe la encomienda. */
public class Destinatario extends Persona {
    public Destinatario(String nombre, String documento, String telefono,
                        String direccion, String ciudad) {
        super(nombre, documento, telefono, direccion, ciudad);
    }
}
