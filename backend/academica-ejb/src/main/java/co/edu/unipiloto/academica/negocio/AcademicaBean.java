package co.edu.unipiloto.academica.negocio;

import co.edu.unipiloto.academica.modelo.*;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.*;

@Stateless
public class AcademicaBean implements AcademicaLocal {
    // GlassFish administra EntityManager y la transacción JTA de cada método EJB.
    @PersistenceContext(unitName = "AcademicaPU")
    private EntityManager em;

    public List<Estudiante> estudiantes() {
        return em.createQuery("SELECT DISTINCT e FROM Estudiante e LEFT JOIN FETCH e.cursos ORDER BY e.codigo", Estudiante.class).getResultList();
    }
    public List<Curso> cursos() {
        return em.createQuery("SELECT DISTINCT c FROM Curso c LEFT JOIN FETCH c.estudiantes ORDER BY c.codigo", Curso.class).getResultList();
    }
    public void guardarEstudiante(String codigo, String nombre, String correo, boolean editar) {
        codigo = Validacion.codigo(codigo);
        nombre = Validacion.texto(nombre, "Nombre", 100); correo = Validacion.correo(correo);
        Estudiante e = em.find(Estudiante.class, codigo, LockModeType.PESSIMISTIC_WRITE);
        if (editar) {
            if (e == null) throw new ReglaNegocioException("El estudiante no existe.");
            e.setNombre(nombre); e.setCorreo(correo);
        } else {
            if (e != null) throw new ReglaNegocioException("Ya existe un estudiante con ese código.");
            em.persist(new Estudiante(codigo, nombre, correo));
        }
        em.flush();
    }
    public void guardarCurso(String codigo, String nombre, int creditos, int semestre, int cupo, boolean editar) {
        codigo = Validacion.codigo(codigo); nombre = Validacion.texto(nombre, "Nombre", 100);
        Validacion.curso(creditos, semestre, cupo);
        Curso c = em.find(Curso.class, codigo, LockModeType.PESSIMISTIC_WRITE);
        if (editar) {
            if (c == null) throw new ReglaNegocioException("El curso no existe.");
            if (cupo < c.getEstudiantes().size()) throw new ReglaNegocioException("El cupo no puede ser menor que el número de inscritos.");
            c.actualizar(nombre, creditos, semestre, cupo);
        } else {
            if (c != null) throw new ReglaNegocioException("Ya existe un curso con ese código.");
            em.persist(new Curso(codigo, nombre, creditos, semestre, cupo));
        }
        em.flush();
    }
    private Curso cursoBloqueado(String codigo) {
        Curso c = em.find(Curso.class, Validacion.codigo(codigo), LockModeType.PESSIMISTIC_WRITE);
        if (c == null) throw new ReglaNegocioException("El curso no existe.");
        return c;
    }
    private Estudiante estudianteBloqueado(String codigo) {
        Estudiante e = em.find(Estudiante.class, Validacion.codigo(codigo), LockModeType.PESSIMISTIC_WRITE);
        if (e == null) throw new ReglaNegocioException("El estudiante no existe.");
        return e;
    }
    public void inscribir(String estudiante, String curso) {
        // Orden de bloqueo común: curso y después estudiante. Serializa el control del cupo.
        Curso c = cursoBloqueado(curso);
        Estudiante e = estudianteBloqueado(estudiante);
        if (e.getCursos().stream().anyMatch(x -> x.getCodigo().equals(c.getCodigo())))
            throw new ReglaNegocioException("El estudiante ya está inscrito en este curso.");
        if (c.getEstudiantes().size() >= c.getEstudiantesAdmitidos())
            throw new ReglaNegocioException("El curso no tiene cupos disponibles.");
        e.getCursos().add(c);
        c.getEstudiantes().add(e);
        em.flush();
    }
    public void retirar(String estudiante, String curso) {
        Curso c = cursoBloqueado(curso);
        Estudiante e = estudianteBloqueado(estudiante);
        boolean eliminado = e.getCursos().removeIf(x -> x.getCodigo().equals(c.getCodigo()));
        if (!eliminado) throw new ReglaNegocioException("No existe esa inscripción.");
        c.getEstudiantes().removeIf(x -> x.getCodigo().equals(e.getCodigo()));
        em.flush();
    }
    public void eliminarEstudiante(String codigo) {
        Estudiante e = estudianteBloqueado(codigo);
        if (!e.getCursos().isEmpty()) throw new ReglaNegocioException("Retira al estudiante de sus cursos antes de eliminarlo.");
        em.remove(e); em.flush();
    }
    public void eliminarCurso(String codigo) {
        Curso c = cursoBloqueado(codigo);
        if (!c.getEstudiantes().isEmpty()) throw new ReglaNegocioException("Retira las inscripciones antes de eliminar el curso.");
        em.remove(c); em.flush();
    }
}
