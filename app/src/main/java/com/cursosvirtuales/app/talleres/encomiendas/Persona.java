package com.cursosvirtuales.app.talleres.encomiendas;

import com.cursosvirtuales.app.R;
import com.cursosvirtuales.app.talleres.TallerActivity;

/** Datos comunes de remitentes y destinatarios. */
public abstract class Persona {

    private String nombre;
    private String documento;
    private String telefono;
    private String direccion;
    private String ciudad;

    protected Persona(String nombre, String documento, String telefono,
                      String direccion, String ciudad) {
        this.nombre = nombre;
        this.documento = documento;
        this.telefono = telefono;
        this.direccion = direccion;
        this.ciudad = ciudad;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    /** Texto de varias líneas para mostrar en el detalle. */
    public String detalle() {
        return nombre + " (CC " + documento + ")\n"
                + "Tel: " + telefono + "\n"
                + direccion + ", " + ciudad;
    }
}
