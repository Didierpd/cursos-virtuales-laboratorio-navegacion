package com.cursosvirtuales.app.cursos;

/**
 * Validaciones de la historia HU-03.
 *
 * Se aisla del Activity para poder probarla con pruebas unitarias
 * sin necesidad de emulador (ver CourseValidatorTest).
 */
public class CourseValidator {

    /** Resultado de validar el formulario de creacion o edicion. */
    public enum Resultado {
        OK,
        NOMBRE_VACIO,
        NOMBRE_MUY_CORTO,
        DESCRIPCION_VACIA,
        FECHAS_INCOMPLETAS,
        FIN_ANTES_DE_INICIO,
        NOMBRE_DUPLICADO
    }

    public static final int LARGO_MINIMO_NOMBRE = 5;

    /** Valida los campos del formulario sin consultar la base de datos. */
    public static Resultado validar(String nombre, String descripcion,
                                    Long fechaInicio, Long fechaFin) {

        if (nombre == null || nombre.trim().isEmpty()) {
            return Resultado.NOMBRE_VACIO;
        }
        if (nombre.trim().length() < LARGO_MINIMO_NOMBRE) {
            return Resultado.NOMBRE_MUY_CORTO;
        }
        if (descripcion == null || descripcion.trim().isEmpty()) {
            return Resultado.DESCRIPCION_VACIA;
        }
        if (fechaInicio == null || fechaFin == null) {
            return Resultado.FECHAS_INCOMPLETAS;
        }
        if (fechaFin <= fechaInicio) {
            return Resultado.FIN_ANTES_DE_INICIO;
        }
        return Resultado.OK;
    }
}
