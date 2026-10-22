package co.edu.unipiloto.academica.web;

import co.edu.unipiloto.academica.negocio.*;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet(urlPatterns = {"/gestion"})
public class AcademicaServlet extends HttpServlet {
    @EJB private AcademicaLocal academica;
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        mostrar(req, res);
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        if (session == null || !String.valueOf(session.getAttribute("csrf")).equals(req.getParameter("csrf"))) {
            res.sendError(403, "Formulario vencido. Recarga la página."); return;
        }
        try {
            String accion = req.getParameter("accion");
            if (accion == null) throw new ReglaNegocioException("Selecciona una acción válida.");
            switch (accion) {
                case "guardarEstudiante":
                    academica.guardarEstudiante(req.getParameter("codigo"), req.getParameter("nombre"), req.getParameter("correo"), "true".equals(req.getParameter("editar"))); break;
                case "guardarCurso":
                    academica.guardarCurso(req.getParameter("codigo"), req.getParameter("nombre"), entero(req,"creditos"), entero(req,"semestre"), entero(req,"cupo"), "true".equals(req.getParameter("editar"))); break;
                case "inscribir": academica.inscribir(req.getParameter("estudiante"),req.getParameter("curso")); break;
                case "retirar": academica.retirar(req.getParameter("estudiante"),req.getParameter("curso")); break;
                case "eliminarEstudiante": academica.eliminarEstudiante(req.getParameter("codigo")); break;
                case "eliminarCurso": academica.eliminarCurso(req.getParameter("codigo")); break;
                default: throw new ReglaNegocioException("Acción desconocida.");
            }
            req.getSession().setAttribute("mensaje", "Cambio guardado correctamente.");
            // POST/Redirect/GET impide repetir un alta al actualizar el navegador.
            res.sendRedirect(req.getContextPath() + "/gestion");
        } catch (ReglaNegocioException error) {
            res.setStatus(400); req.setAttribute("error", error.getMessage()); mostrar(req,res);
        } catch (EJBException error) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING,"No se pudo completar la transacción",error);
            res.setStatus(409);
            req.setAttribute("error","No se pudo guardar el cambio. Puede existir un conflicto concurrente; actualiza e inténtalo de nuevo.");
            mostrar(req,res);
        }
    }
    private int entero(HttpServletRequest req,String campo) {
        try { return Integer.parseInt(req.getParameter(campo)); }
        catch (NumberFormatException error) { throw new ReglaNegocioException("El campo " + campo + " debe ser un número entero."); }
    }
    private void mostrar(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
        res.setContentType("text/html;charset=UTF-8"); res.setHeader("Cache-Control","no-store");
        HttpSession session=req.getSession();
        if (session.getAttribute("csrf")==null) session.setAttribute("csrf",UUID.randomUUID().toString());
        req.setAttribute("estudiantes",academica.estudiantes()); req.setAttribute("cursos",academica.cursos());
        req.setAttribute("mensaje",session.getAttribute("mensaje")); session.removeAttribute("mensaje");
        req.getRequestDispatcher("/WEB-INF/studentInfo.jsp").forward(req,res);
    }
}
