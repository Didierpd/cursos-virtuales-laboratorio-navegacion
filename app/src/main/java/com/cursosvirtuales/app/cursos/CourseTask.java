package com.cursosvirtuales.app.cursos;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * HU-06 - Crear una tarea con enunciado, fecha limite y puntaje.
 *
 * Representa una fila de la tabla TAREA, asociada siempre a un curso.
 */
public class CourseTask {

    public static final long SIN_ID = -1L;

    private long id;
    private long cursoId;
    private String titulo;
    private String enunciado;
    private long fechaLimite;
    private int puntaje;

    public CourseTask(long id, long cursoId, String titulo, String enunciado,
                       long fechaLimite, int puntaje) {
        this.id = id;
        this.cursoId = cursoId;
        this.titulo = titulo;
        this.enunciado = enunciado;
        this.fechaLimite = fechaLimite;
        this.puntaje = puntaje;
    }

    public long getId() { return id; }
    public long getCursoId() { return cursoId; }
    public String getTitulo() { return titulo; }
    public String getEnunciado() { return enunciado; }
    public long getFechaLimite() { return fechaLimite; }
    public int getPuntaje() { return puntaje; }

    public void setId(long id) { this.id = id; }
    public void setCursoId(long cursoId) { this.cursoId = cursoId; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }
    public void setFechaLimite(long fechaLimite) { this.fechaLimite = fechaLimite; }
    public void setPuntaje(int puntaje) { this.puntaje = puntaje; }

    /** Formato usado en pantalla para mostrar la fecha limite. */
    public static String formatearFecha(long millis) {
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", new Locale("es", "CO"));
        return formato.format(new Date(millis));
    }

    /** Texto que se muestra en el listado de tareas. */
    @Override
    public String toString() {
        return titulo + "  (" + puntaje + " pts)\nEntrega: " + formatearFecha(fechaLimite);
    }
}
