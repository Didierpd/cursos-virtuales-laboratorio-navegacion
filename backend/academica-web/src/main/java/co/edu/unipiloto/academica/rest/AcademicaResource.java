package co.edu.unipiloto.academica.rest;

import co.edu.unipiloto.academica.modelo.*;
import co.edu.unipiloto.academica.negocio.*;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.json.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

/** Contrato JSON independiente de las entidades y sus relaciones bidireccionales. */
@RequestScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AcademicaResource {
    @EJB private AcademicaLocal servicio;

    @GET @Path("cursos")
    public JsonArray cursos() {
        JsonArrayBuilder items = Json.createArrayBuilder();
        for (Curso c : servicio.cursos()) {
            items.add(Json.createObjectBuilder().add("codigo", c.getCodigo())
                .add("nombre", c.getNombre()).add("creditos", c.getCreditos())
                .add("semestre", c.getSemestre()).add("cupo", c.getEstudiantesAdmitidos())
                .add("inscritos", c.getEstudiantes().size()));
        }
        return items.build();
    }

    @GET @Path("estudiantes")
    public JsonArray estudiantes() {
        JsonArrayBuilder items = Json.createArrayBuilder();
        for (Estudiante e : servicio.estudiantes()) {
            JsonArrayBuilder cursos = Json.createArrayBuilder();
            for (Curso c : e.getCursos()) cursos.add(c.getCodigo());
            items.add(Json.createObjectBuilder().add("codigo", e.getCodigo())
                .add("nombre", e.getNombre()).add("correo", e.getCorreo()).add("cursos", cursos));
        }
        return items.build();
    }

    @POST @Path("cursos")
    public Response crearCurso(JsonObject datos) {
        servicio.guardarCurso(texto(datos, "codigo"), texto(datos, "nombre"),
            entero(datos, "creditos"), entero(datos, "semestre"), entero(datos, "cupo"), false);
        return creado("Curso registrado.");
    }

    @POST @Path("estudiantes")
    public Response crearEstudiante(JsonObject datos) {
        servicio.guardarEstudiante(texto(datos, "codigo"), texto(datos, "nombre"), texto(datos, "correo"), false);
        return creado("Estudiante registrado.");
    }

    @POST @Path("inscripciones")
    public Response inscribir(JsonObject datos) {
        servicio.inscribir(texto(datos, "estudiante"), texto(datos, "curso"));
        return creado("Inscripción registrada.");
    }

    private static Response creado(String mensaje) {
        return Response.status(Response.Status.CREATED)
            .entity(Json.createObjectBuilder().add("mensaje", mensaje).build()).build();
    }
    private static String texto(JsonObject datos, String campo) {
        if (datos == null || !(datos.get(campo) instanceof JsonString))
            throw new BadRequestException("El campo " + campo + " debe ser texto.");
        return datos.getString(campo);
    }
    private static int entero(JsonObject datos, String campo) {
        if (datos == null || !(datos.get(campo) instanceof JsonNumber))
            throw new BadRequestException("El campo " + campo + " debe ser entero.");
        try { return datos.getJsonNumber(campo).intValueExact(); }
        catch (ArithmeticException ex) { throw new BadRequestException("El campo " + campo + " debe ser entero."); }
    }
}
