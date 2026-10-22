package com.cursosvirtuales.app.talleres.encomiendas;

import com.cursosvirtuales.app.R;
import com.cursosvirtuales.app.talleres.TallerActivity;

import java.util.Locale;

/** Paquete enviado de un remitente a un destinatario. */
public class Encomienda {

    private long id;
    private String guia;
    private String descripcion;
    private double pesoKg;
    private double valorDeclarado;
    private String tipoEnvio;       // "Normal" o "Express"
    private String fechaRegistro;   // yyyy-MM-dd HH:mm
    private Remitente remitente;
    private Destinatario destinatario;

    public Encomienda(String guia, String descripcion, double pesoKg, double valorDeclarado,
                      String tipoEnvio, String fechaRegistro,
                      Remitente remitente, Destinatario destinatario) {
        this.guia = guia;
        this.descripcion = descripcion;
        this.pesoKg = pesoKg;
        this.valorDeclarado = valorDeclarado;
        this.tipoEnvio = tipoEnvio;
        this.fechaRegistro = fechaRegistro;
        this.remitente = remitente;
        this.destinatario = destinatario;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getGuia() { return guia; }
    public String getDescripcion() { return descripcion; }
    public double getPesoKg() { return pesoKg; }
    public double getValorDeclarado() { return valorDeclarado; }
    public String getTipoEnvio() { return tipoEnvio; }
    public String getFechaRegistro() { return fechaRegistro; }
    public Remitente getRemitente() { return remitente; }
    public Destinatario getDestinatario() { return destinatario; }

    /** Una línea corta para la lista de resultados. */
    @Override
    public String toString() {
        return guia + " · " + descripcion + "\n"
                + remitente.getCiudad() + " → " + destinatario.getCiudad()
                + " · " + tipoEnvio;
    }

    /** Detalle completo para el diálogo. */
    public String detalle() {
        return "Guía: " + guia + "\n"
                + "Fecha: " + fechaRegistro + "\n"
                + "Contenido: " + descripcion + "\n"
                + String.format(Locale.getDefault(), "Peso: %.2f kg%n", pesoKg)
                + String.format(Locale.getDefault(), "Valor declarado: $%,.0f%n", valorDeclarado)
                + "Tipo de envío: " + tipoEnvio + "\n\n"
                + "REMITENTE\n" + remitente.detalle() + "\n\n"
                + "DESTINATARIO\n" + destinatario.detalle();
    }
}
