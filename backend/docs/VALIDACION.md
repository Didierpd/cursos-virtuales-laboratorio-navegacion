# Validación realizada

Fecha: 13 de septiembre de 2026.

- Compilación inicial: `mvn -o package`, resultado `BUILD SUCCESS`.
- Compilación limpia con JDK 8: `JAVA_HOME=/ruta/jdk8 mvn -o clean package`, resultado `BUILD SUCCESS` en los cuatro proyectos del reactor.
- Base: Apache Derby 10.10.2.0, esquema `APP`, base `AcademicaDB`; tablas creadas con el SQL del proyecto.
- Servidor: GlassFish 4.1 con Temurin Java 1.8.0_502.
- Despliegue del EAR: correcto; aplicación `GestionAcademicaJPA`, contexto `/academica`.
- Pruebas HTTP: **25 comprobaciones correctas** mediante `scripts/pruebas_http.py` contra el servidor real.
- Persistencia tras redespliegue: se crearon estudiante, curso e inscripción temporal; se redesplegó el EAR y una sesión nueva recuperó la inscripción. Después se retiró y se eliminaron los registros temporales.

## Cobertura de las pruebas HTTP

Página JSP; token CSRF inválido; registro de dos estudiantes; escape HTML; estudiante duplicado; creación de dos cursos; créditos inválidos; tres inscripciones que demuestran ambos sentidos de la relación; inscripción duplicada; cupo agotado; reducción de cupo inválida; eliminación protegida de estudiante y curso; edición de estudiante y curso; recuperación desde una sesión nueva; acceso directo a JSP bloqueado; CSS disponible; retiros y eliminación final.

## Límites

No hay pruebas unitarias JUnit; Maven comprueba compilación y empaquetado, y el script Python verifica integración. No se realizó una prueba de carga concurrente ni una revisión visual automatizada. NetBeans 8.0.2 no está instalado en el equipo de validación: se proporciona la configuración de Maven externo para abrir el proyecto y se verificó el artefacto en GlassFish 4.1.
