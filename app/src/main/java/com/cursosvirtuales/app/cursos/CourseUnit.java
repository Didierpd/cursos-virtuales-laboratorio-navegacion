package com.cursosvirtuales.app.cursos;

/**
 * HU-05 - Organizar el curso en unidades o modulos.
 *
 * Representa una fila de la tabla UNIDAD. El campo orden define la
 * secuencia en la que el estudiante ve las unidades dentro del curso.
 */
public class CourseUnit {

    public static final long SIN_ID = -1L;

    private long id;
    private long cursoId;
    private String nombre;
    private String recurso;
    private int orden;

    public CourseUnit(long id, long cursoId, String nombre, String recurso, int orden) {
        this.id = id;
        this.cursoId = cursoId;
        this.nombre = nombre;
        this.recurso = recurso;
        this.orden = orden;
    }

    public long getId() { return id; }
    public long getCursoId() { return cursoId; }
    public String getNombre() { return nombre; }
    public String getRecurso() { return recurso; }
    public int getOrden() { return orden; }

    public void setId(long id) { this.id = id; }
    public void setCursoId(long cursoId) { this.cursoId = cursoId; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setRecurso(String recurso) { this.recurso = recurso; }
    public void setOrden(int orden) { this.orden = orden; }

    /** Texto que se muestra en el listado de unidades. */
    @Override
    public String toString() {
        String base = (orden + 1) + ". " + nombre;
        if (recurso != null && !recurso.trim().isEmpty()) {
            base += "\n" + recurso;
        }
        return base;
    }
}
