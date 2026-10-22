package co.edu.unipiloto.academica.modelo;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.*;

@Entity
@Table(name = "CURSO")
public class Curso implements Serializable {
    @Id @Column(name = "CODIGO", length = 20)
    private String codigo;
    @Column(name = "NOMBRE", nullable = false, length = 100)
    private String nombre;
    @Column(name = "CREDITOS", nullable = false)
    private int creditos;
    @Column(name = "SEMESTRE", nullable = false)
    private int semestre;
    @Column(name = "ESTUDIANTES_ADMITIDOS", nullable = false)
    private int estudiantesAdmitidos;
    // Lado inverso; mappedBy apunta al atributo cursos de Estudiante.
    @ManyToMany(mappedBy = "cursos")
    private Set<Estudiante> estudiantes = new LinkedHashSet<>();

    public Curso() {}
    public Curso(String codigo, String nombre, int creditos, int semestre, int admitidos) {
        this.codigo = codigo; actualizar(nombre, creditos, semestre, admitidos);
    }
    public void actualizar(String nombre, int creditos, int semestre, int admitidos) {
        this.nombre = nombre; this.creditos = creditos; this.semestre = semestre;
        this.estudiantesAdmitidos = admitidos;
    }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public int getCreditos() { return creditos; }
    public int getSemestre() { return semestre; }
    public int getEstudiantesAdmitidos() { return estudiantesAdmitidos; }
    public Set<Estudiante> getEstudiantes() { return estudiantes; }
}
