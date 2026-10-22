# Repositorios del curso y estado de integración

| Repositorio | Contenido | Relación con la entrega Retrofit |
| --- | --- | --- |
| [CursosVirtualesApp](https://github.com/Didierpd/cursos-virtuales-laboratorio-navegacion) | App Android de profesores y estudiantes, HU locales con SQLite y pantalla de servicios REST | Proyecto Android principal |
| [GestionAcademicaJPA](https://github.com/Didierpd/GestionAcademicaJPA) | Backend Java EE, EJB, JPA, Derby, JSP y API REST académica | La app consume sus GET y POST; el código del backend permanece en su propio repositorio |
| [ConstraintLayoutsLab](https://github.com/Didierpd/ConstraintLayoutsLab) | Ejercicios de ConstraintLayout y formularios Android | Laboratorio independiente; sus pantallas no se incorporaron a la app principal |
| [SQLite encomiendas](https://github.com/nicolasmoreno2006/laboratorio-sqlite-encomiendas) | ZIP `encomiendas-sqlite.zip`: registro, consulta y búsqueda de encomiendas; remitentes, destinatarios y persistencia SQLite | Referencia adicional revisada; no se incorporaron sus pantallas ni sus tablas al sistema académico |

## Qué contiene actualmente el proyecto

Este repositorio contiene el código de la aplicación académica y la integración
Retrofit con el backend. No contiene copias de los otros tres repositorios,
submódulos Git ni un ZIP que reúna todos los proyectos.

Conectar Android con un backend permite intercambiar datos entre dos aplicaciones;
no reúne sus archivos ni convierte sus bases de datos en una sola. Los cursos
SQLite previos y los cursos remotos de JPA siguen siendo conjuntos de datos distintos.

El laboratorio de encomiendas usa otro dominio: envíos, remitentes y destinatarios.
Sus técnicas SQLite pueden servir de referencia para el proyecto académico.
Agregar su enlace a esta lista no equivale a integrar sus funcionalidades.

Para reunir todo en un repositorio se pueden guardar proyectos en carpetas
separadas. Para obtener una sola app académica hay que adaptar las funcionalidades
pertinentes y unificar navegación, modelos y persistencia. Esa elección queda
pendiente de la definición del alcance con el usuario.
