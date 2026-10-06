package com.cursosvirtuales.app.cursos;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * HU-03 - Crear un curso con su nombre, descripcion y fechas.
 *
 * Representa una fila de la tabla CURSO. Las fechas se guardan como
 * milisegundos (long) para poder compararlas y ordenarlas en SQL.
 */
public class Course {

    public static final long SIN_ID = -1L;

    private long id;
    private String nombre;
    private String descripcion;
    private long fechaInicio;
    private long fechaFin;
    private String docenteId;

    public Course(long id, String nombre, String descripcion,
                  long fechaInicio, long fechaFin, String docenteId) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.docenteId = docenteId;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public long getFechaInicio() { return fechaInicio; }
    public long getFechaFin() { return fechaFin; }
    public String getDocenteId() { return docenteId; }

    public void setId(long id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setFechaInicio(long fechaInicio) { this.fechaInicio = fechaInicio; }
    public void setFechaFin(long fechaFin) { this.fechaFin = fechaFin; }
    public void setDocenteId(String docenteId) { this.docenteId = docenteId; }

    /** Formato usado en pantalla para mostrar las fechas del curso. */
    public static String formatearFecha(long millis) {
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", new Locale("es", "CO"));
        return formato.format(new Date(millis));
    }

    /** Texto que se muestra en el listado de cursos. */
    @Override
    public String toString() {
        return nombre + "\n" + formatearFecha(fechaInicio) + "  -  " + formatearFecha(fechaFin);
    }
}
