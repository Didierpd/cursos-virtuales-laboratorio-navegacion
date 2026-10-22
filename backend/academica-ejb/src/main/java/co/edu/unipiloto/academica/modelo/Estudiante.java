package co.edu.unipiloto.academica.modelo;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.*;

@Entity
@Table(name = "ESTUDIANTE")
public class Estudiante implements Serializable {
    @Id @Column(name = "CODIGO", length = 20)
    private String codigo;
    @Column(name = "NOMBRE", nullable = false, length = 100)
    private String nombre;
    @Column(name = "CORREO", nullable = false, length = 150)
    private String correo;

    // Lado propietario: JPA escribe las dos llaves foráneas en esta tabla intermedia.
    @ManyToMany
    @JoinTable(name = "ESTUDIANTE_CURSO",
        joinColumns = @JoinColumn(name = "ESTUDIANTE_CODIGO", referencedColumnName = "CODIGO"),
        inverseJoinColumns = @JoinColumn(name = "CURSO_CODIGO", referencedColumnName = "CODIGO"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"ESTUDIANTE_CODIGO", "CURSO_CODIGO"}))
    private Set<Curso> cursos = new LinkedHashSet<>();

    public Estudiante() {}
    public Estudiante(String codigo, String nombre, String correo) {
        this.codigo = codigo; this.nombre = nombre; this.correo = correo;
    }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public Set<Curso> getCursos() { return cursos; }
}
