package com.cursosvirtuales.app.cursos;

/**
 * Validaciones de la historia HU-06 - Crear una tarea con enunciado,
 * fecha limite y puntaje.
 */
public class TaskValidator {

    public enum Resultado {
        OK,
        TITULO_VACIO,
        ENUNCIADO_VACIO,
        FECHA_LIMITE_VACIA,
        PUNTAJE_INVALIDO
    }

    public static final int PUNTAJE_MAXIMO = 100;

    public static Resultado validar(String titulo, String enunciado,
                                     Long fechaLimite, String puntajeTexto) {

        if (titulo == null || titulo.trim().isEmpty()) {
            return Resultado.TITULO_VACIO;
        }
        if (enunciado == null || enunciado.trim().isEmpty()) {
            return Resultado.ENUNCIADO_VACIO;
        }
        if (fechaLimite == null) {
            return Resultado.FECHA_LIMITE_VACIA;
        }
        if (!esPuntajeValido(puntajeTexto)) {
            return Resultado.PUNTAJE_INVALIDO;
        }
        return Resultado.OK;
    }

    private static boolean esPuntajeValido(String puntajeTexto) {
        if (puntajeTexto == null || puntajeTexto.trim().isEmpty()) {
            return false;
        }
        try {
            int puntaje = Integer.parseInt(puntajeTexto.trim());
            return puntaje > 0 && puntaje <= PUNTAJE_MAXIMO;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
