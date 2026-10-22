package co.edu.unipiloto.academica.negocio;
import java.util.Locale;
public final class Validacion {
    private Validacion() {}
    public static String codigo(String valor) {
        String codigo = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (!codigo.matches("[A-Z0-9_-]{1,20}"))
            throw new ReglaNegocioException("El código debe tener de 1 a 20 letras, números, guiones o guiones bajos.");
        return codigo;
    }
    public static String texto(String valor, String campo, int limite) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.isEmpty() || texto.length() > limite)
            throw new ReglaNegocioException(campo + ": escribe entre 1 y " + limite + " caracteres.");
        return texto;
    }
    public static String correo(String valor) {
        String correo = texto(valor, "Correo", 150);
        if (!correo.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new ReglaNegocioException("Escribe un correo válido.");
        return correo;
    }
    public static void curso(int creditos, int semestre, int cupo) {
        if (creditos < 1 || creditos > 30 || semestre < 1 || semestre > 20 || cupo < 1 || cupo > 1000)
            throw new ReglaNegocioException("Usa créditos de 1 a 30, semestre de 1 a 20 y cupo de 1 a 1000.");
    }
}
