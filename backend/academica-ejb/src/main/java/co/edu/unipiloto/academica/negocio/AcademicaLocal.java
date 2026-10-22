package co.edu.unipiloto.academica.negocio;
import java.util.List;
import javax.ejb.Local;
import co.edu.unipiloto.academica.modelo.*;
@Local
public interface AcademicaLocal {
    List<Estudiante> estudiantes();
    List<Curso> cursos();
    void guardarEstudiante(String codigo, String nombre, String correo, boolean editar);
    void guardarCurso(String codigo, String nombre, int creditos, int semestre, int cupo, boolean editar);
    void inscribir(String estudiante, String curso);
    void retirar(String estudiante, String curso);
    void eliminarEstudiante(String codigo);
    void eliminarCurso(String codigo);
}
