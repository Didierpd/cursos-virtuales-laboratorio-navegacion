# API REST para el cliente Android

JAX-RS de Java EE 7, sin dependencias adicionales. `RestApplication` registra
`/api`; `AcademicaResource` delega al EJB existente y construye JSON sin ciclos.
Los registros se guardan en la misma base Derby que los formularios JSP.

Compilar y desplegar según el README antes de usar las rutas. No se cambian
las tablas ni se recrea la base. La API es para laboratorio y no incorpora
autenticación ni autorización por rol.

## Servicios implementados

Base: `http://localhost:8080/academica/api/`. Solicitudes y respuestas JSON.

| Método | Ruta | Función | Respuesta correcta |
| --- | --- | --- | --- |
| GET | `cursos` | Consultar cursos, cupos e inscritos | 200, lista |
| GET | `estudiantes` | Consultar estudiantes y códigos de sus cursos | 200, lista |
| POST | `cursos` | Registrar un curso | 201, mensaje |
| POST | `estudiantes` | Registrar un estudiante | 201, mensaje |
| POST | `inscripciones` | Inscribir estudiante en un curso | 201, mensaje |

Cuerpos de los POST, respectivamente:

```json
{"codigo":"REST-101","nombre":"Servicios REST","creditos":3,"semestre":2,"cupo":20}
```

```json
{"codigo":"EST-REST","nombre":"Ana Pérez","correo":"ana@example.com"}
```

```json
{"estudiante":"EST-REST","curso":"REST-101"}
```

Respuesta de registro: `{"mensaje":"Curso registrado."}`. Los errores de campos,
duplicados, inscripción repetida y falta de cupo devuelven HTTP 400 con `mensaje`.
Los fallos internos devuelven HTTP 500 y un mensaje general; el detalle se registra
solo en el servidor. No se serializan directamente las relaciones bidireccionales JPA.

## Prueba rápida

```bash
curl http://localhost:8080/academica/api/cursos
curl -i -H 'Content-Type: application/json' -d '{"codigo":"REST-101","nombre":"Servicios REST","creditos":3,"semestre":2,"cupo":20}' http://localhost:8080/academica/api/cursos
python3 scripts/pruebas_rest.py
python3 scripts/pruebas_http.py
```

El script REST crea códigos únicos, comprueba altas, lectura desde la base,
inscripciones, duplicados, cupos, campos y JSON inválidos y limpia sus datos
mediante los formularios existentes.

Cliente Android: https://github.com/Didierpd/cursos-virtuales-laboratorio-navegacion
Ver `docs/taller-retrofit.md` para configurar el emulador o teléfono físico.

## Validación del taller (21 de octubre de 2026)

- Android: `assembleDebug testDebugUnitTest lintDebug`, BUILD SUCCESSFUL.
- Seis pruebas Retrofit/MockWebServer correctas, sin fallos.
- Backend: `mvn -o package`, BUILD SUCCESS; EAR desplegado en GlassFish 4.1 con Java 8.
- `scripts/pruebas_rest.py`: 43 comprobaciones correctas contra GlassFish y Derby reales.
- `scripts/pruebas_http.py`: 25 comprobaciones correctas del sitio JSP existente.
- Los scripts eliminaron sus registros temporales al finalizar.

Lint conserva advertencias del proyecto; no hay errores. No se hizo una revisión
visual de la nueva pantalla en emulador ni se recibió la presentación del profesor.
