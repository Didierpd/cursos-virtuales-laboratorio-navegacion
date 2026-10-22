package co.edu.unipiloto.academica.negocio;
import javax.ejb.ApplicationException;
@ApplicationException(rollback = true)
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) { super(mensaje); }
}
