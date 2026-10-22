package co.edu.unipiloto.academica.rest;

import co.edu.unipiloto.academica.negocio.ReglaNegocioException;
import javax.json.Json;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;
import java.util.logging.*;

@Provider
public class RestErrorMapper implements ExceptionMapper<Exception> {
    private static final Logger LOG = Logger.getLogger(RestErrorMapper.class.getName());
    @Override public Response toResponse(Exception ex) {
        // Los recursos EJB pueden envolver excepciones de JAX-RS.
        while (ex instanceof javax.ejb.EJBException && ex.getCause() instanceof Exception) {
            ex = (Exception) ex.getCause();
        }
        int estado = 500;
        String mensaje = "No se pudo completar la operación en el servidor.";
        if (ex instanceof ReglaNegocioException) {
            estado = 400; mensaje = ex.getMessage();
        } else if (ex instanceof javax.json.stream.JsonParsingException) {
            estado = 400; mensaje = "El cuerpo debe contener JSON válido.";
        } else if (ex instanceof WebApplicationException) {
            estado = ((WebApplicationException) ex).getResponse().getStatus();
            mensaje = estado == 400 ? "JSON o campos de la solicitud inválidos." : "Solicitud HTTP rechazada.";
        }
        if (estado == 500) LOG.log(Level.SEVERE, "Fallo en la API académica", ex);
        return Response.status(estado).type(MediaType.APPLICATION_JSON)
            .entity(Json.createObjectBuilder().add("mensaje", mensaje).build()).build();
    }
}
