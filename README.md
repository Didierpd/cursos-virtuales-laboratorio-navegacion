# Cursos Virtuales — HU-05 a HU-12

Aplicación Android nativa (Java/Kotlin, SQLite). Conserva login, registro, perfil,
creación/edición de cursos e inscripciones del ZIP original.

## Laboratorio de navegación

[Propuesta de navegación — List views and adapters](docs/laboratorio-navegacion.md).
Incluye la respuesta al ejercicio de la diapositiva 8, clasificación de pantallas,
mapa de navegación por rol y uso de listas, adaptadores e intents.

## Ejecutar

1. Abrir esta carpeta en Android Studio.
2. Instalar Android SDK Platform 35. Usar JDK 17 o 21 para Gradle.
3. Sincronizar y ejecutar `app` en Android 7.0 (API 24) o superior.

Configuración comprobada: Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 1.9.24.
El `local.properties` se genera localmente con la ruta de tu SDK; no se incluye
en el ZIP portable. También se puede definir `ANDROID_HOME`.

```bash
./gradlew assembleDebug
./gradlew assembleDebugAndroidTest
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.cursosvirtuales.app.LearningFlowTest
./gradlew lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Cuentas de demostración

| Rol | Usuario | Contraseña |
| --- | --- | --- |
| Docente | docente | docente123 |
| Estudiante | estudiante | estudiante123 |

## Recorrido para verificar las historias

1. Docente → Mis cursos → crear un curso → abrirlo.
2. HU-05: Unidades → crear unidades con material (texto o enlace). Seleccionar
   una unidad y usar Subir/Bajar. La pulsación larga permite eliminar.
3. HU-06: Tareas → Nueva tarea → título, enunciado, fecha límite y puntaje de
   1 a 100. El vencimiento corresponde a las 23:59:59.999 de la fecha elegida,
   según la zona horaria del dispositivo.
4. Cerrar sesión. Estudiante → Cursos disponibles → inscribirse.
5. HU-05/HU-07: Mis cursos y tareas → abrir el curso para consultar unidades
   ordenadas o abrir una tarea. Las tareas muestran fecha y estado:
   Pendiente/Vigente, Vencida, Entregada o Calificada. Solo aparecen las de
   cursos en los que el estudiante está inscrito.
6. HU-08: abrir una tarea vigente → Seleccionar archivos → elegir uno o varios
   en el selector Android → Entregar tarea → confirmar. Se registra fecha y
   hora. Se admiten hasta 10 archivos de 20 MB cada uno, no vacíos. Los archivos
   se copian al almacenamiento privado y se conservan al reiniciar la app.
   No se permite duplicar una entrega. Las entregas después del vencimiento
   requieren que el docente active la política de entregas tardías (HU-11).
7. Cerrar sesión. Docente → curso → Tareas → abrir la tarea → consultar entregas
   y abrir archivos → Calificar entrega. La nota debe estar entre 0 y el puntaje
   máximo; el comentario es obligatorio. Se puede editar la nota y el comentario.
8. Estudiante → tarea entregada → consultar la calificación y retroalimentación.

## Persistencia y alcance

Usuarios, cursos, unidades, tareas, entregas y calificaciones se almacenan en
SQLite. Las migraciones a versión 6 conservan los datos anteriores. Los archivos
se guardan en almacenamiento privado y se comparten temporalmente con el visor
mediante FileProvider. Abrir un adjunto requiere un visor instalado compatible.

Es una aplicación local, como el proyecto original: los roles comparten datos
al cambiar de cuenta **en el mismo dispositivo**. No sincroniza entre teléfonos
ni incluye un servidor. Desinstalar la app elimina los datos locales.

## Pruebas incluidas

`LearningFlowTest` usa base de datos, sesión y archivos separados de los reales.
Comprueba entrega con varios archivos, persistencia, calificación y edición,
rechazo de permisos incorrectos, vencimiento, campos y notas inválidos,
orden de unidades tras eliminar/insertar y migración desde la versión 3.

## Nuevas historias HU-10 a HU-15

### HU-10 — Mis calificaciones

Estudiante → Mis calificaciones (también accesible desde Mis cursos y tareas).
Muestra las tareas agrupadas por curso, las notas y los comentarios del docente.
Las consultas usan el usuario de la sesión; no permiten elegir otro estudiante.
El acumulado es la suma de notas guardadas frente a los puntos de todas las
 tareas publicadas. También se muestra el total de puntos de las tareas ya
calificadas y el número de tareas calificadas. Las pendientes figuran como
sin calificación, sin asignarles una nota de cero.

### HU-11 — Política de entregas fuera de plazo

Docente → Mis cursos → abrir curso → Aceptar entregas tardías en este curso.
La política inicia desactivada. Si se activa, los estudiantes inscritos pueden
entregar después de la fecha límite y la entrega muestra el retraso en días,
horas, minutos y segundos. Las entregas puntuales muestran su anticipación.
Se vuelve a comprobar la política y el plazo al guardar, después de copiar los
archivos. Cambiar la política afecta nuevas entregas: no borra entregas previas.

### HU-12 — Prácticas de laboratorio

Docente → curso → Laboratorio → Nueva práctica. Completar título, objetivo y
guía. Se pueden adjuntar hasta 10 archivos (20 MB por archivo). La guía puede
ser suficiente por sí sola; los recursos adjuntos son opcionales.
Al publicar, el laboratorio queda visible para los estudiantes inscritos desde
el mismo curso. Se conservan la guía y copias privadas de los recursos al cerrar
la aplicación. Abrir un recurso requiere un visor compatible instalado.

Las pantallas de creación y los cambios de política comprueban que el docente
sea dueño del curso. Un estudiante no inscrito no puede consultar laboratorios.

### HU-13 — Consultar guía y recursos

El estudiante inscrito abre el botón Laboratorio del curso y consulta la guía,
el objetivo y los recursos de cada práctica publicada. Los recursos se abren
desde el almacenamiento privado mediante `FileProvider`; un estudiante que no
está inscrito no puede consultar la práctica.

### HU-14 — Entregar informe de resultados

La entrega de archivos de una tarea se utiliza también para el informe de
laboratorio. El estudiante puede seleccionar hasta 10 archivos, cada uno de
hasta 20 MB, confirmar el envío y consultar la fecha y hora registrada. La
entrega se guarda junto con sus archivos y no puede duplicarse para la misma
tarea. El docente puede abrir los archivos recibidos.

### HU-15 — Revisar y validar informes

El docente abre una tarea y ve las entregas de los estudiantes, sus archivos,
estado y diferencia frente al plazo. Puede registrar una nota dentro del
puntaje máximo y una retroalimentación obligatoria; también puede editar ambos.
El estudiante ve después su nota y comentario en Mis calificaciones. Cada
estudiante solo consulta su propio informe y el docente solo puede calificar
tareas de cursos que posee.
