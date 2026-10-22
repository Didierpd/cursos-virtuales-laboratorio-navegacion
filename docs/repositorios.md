# Integración de los cuatro repositorios

Esta entrega instala **una sola app Android**, `com.cursosvirtuales.app`, con
un menú, una sesión y navegación compartida. El código del servidor también
está incluido en `backend/`, para descargar la entrega desde un solo repositorio.

| Origen | Integración en esta entrega |
| --- | --- |
| [Cursos Virtuales](https://github.com/Didierpd/cursos-virtuales-laboratorio-navegacion) | Login, perfiles y HU de cursos, unidades, tareas, entregas, calificaciones y laboratorios; menú principal y Retrofit |
| [GestionAcademicaJPA](https://github.com/Didierpd/GestionAcademicaJPA) | Código completo del backend en `backend/`; cursos, estudiantes e inscripciones accesibles desde Servicios REST |
| [ConstraintLayoutsLab](https://github.com/Didierpd/ConstraintLayoutsLab) | Cuatro ejercicios ConstraintLayout y formularios de correo y entrega en `talleres/layouts`, accesibles desde Interfaces y formularios |
| [SQLite encomiendas](https://github.com/nicolasmoreno2006/laboratorio-sqlite-encomiendas) | Modelos, registro, consulta, búsqueda y SQLite adaptados en `talleres/encomiendas`, accesibles desde Encomiendas · SQLite |

## Adaptaciones

Se cambiaron los paquetes y se prefijaron recursos para evitar colisiones.
Los talleres usan la sesión de Cursos Virtuales y el botón de regreso al menú.
El formulario de entrega guarda la última entrega por usuario; el formulario
de correo guarda un borrador por usuario, sin afirmar que envía mensajes.
Se mejoró la validación de URL GitHub y números de encomiendas.

El código de encomiendas proviene del ZIP `encomiendas-sqlite.zip` del repositorio
de nicolasmoreno2006, blob `f3e2631e433206075807e5f619007c61cf30c521`.
Se reconoce esa procedencia; no se atribuye su autoría original a este proyecto.

## Ejecución

Consultar [la guía de presentación](presentacion.md). La app es un solo APK;
el servidor Java EE se ejecuta en el computador y Android lo consulta por HTTP.
El backend no puede ejecutarse dentro del APK. En el emulador su URL es
`http://10.0.2.2:8080/academica/api/`.

Los datos académicos locales, las encomiendas locales y la gestión remota
conservan su almacenamiento correspondiente. No se implementa sincronización
automática entre esas bases. Los talleres se reúnen como secciones de una sola
app, conservando sus funciones originales.
