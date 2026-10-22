# Presentar la app integrada

## En este computador

La app instalada se llama **Cursos Virtuales**. Para volver a iniciar la
presentación, abrir **Presentar Cursos Virtuales** desde el escritorio,
o ejecutar desde la raíz del repositorio:

```bash
./scripts/presentar.sh
```

El acceso comprueba el backend, inicia los servicios si es necesario, carga
los ejemplos REST que falten, abre el emulador y reinstala el APK conservando
los datos. Requiere la base y el recurso JDBC configurados como describe
`backend/README.md`; no vuelve a crear tablas ni borra registros.

La configuración predeterminada usa Android SDK en `~/Android/Sdk`, GlassFish
en `~/Aplicaciones/glassfish41/glassfish4` y Java 8 en `~/Aplicaciones/java8`.
Se puede adaptar con `ANDROID_HOME`, `GLASSFISH_HOME`, `JAVA8_HOME` y `CURSOS_AVD`.

## Cuentas

| Rol | Usuario | Contraseña |
| --- | --- | --- |
| Docente | docente | docente123 |
| Estudiante | estudiante | estudiante123 |

## Recorrido sugerido (5 a 8 minutos)

1. Entrar como docente. El menú reúne todos los talleres en un solo APK.
2. Pulsar **Cargar ejemplos de presentación** si se necesitan datos. Añade
   un curso, una unidad, una tarea vigente, un laboratorio y una encomienda;
   inscribe la cuenta estudiante en el curso. Repetirlo no duplica esos ejemplos.
3. Abrir **Mis cursos**, seleccionar **Demo · Desarrollo de aplicaciones móviles**
   y mostrar unidades, tarea y guía de laboratorio.
4. Regresar y abrir **Servicios REST**. Usar
   `http://10.0.2.2:8080/academica/api/`, consultar cursos y estudiantes.
   Mostrar `DEMO-REST-101` y `DEMO-EST-001`.
5. Para demostrar POST en vivo, registrar un curso con un código nuevo
   (por ejemplo `EXPO-101`), nombre, 3 créditos, semestre 2 y cupo 10.
   Volver a **Consultar cursos · GET** para mostrar el registro persistido.
   Repetir el mismo código permite mostrar el error del backend.
6. Abrir **Encomiendas · SQLite**, bajar a **Consultar encomiendas** y mostrar
   `ENC-DEMO-001`. Buscar por guía o documento `100100`; tocar el resultado
   muestra remitente, destinatario y datos del envío. También se puede
   registrar una nueva encomienda completando ambos participantes y el paquete.
7. Abrir **Interfaces y formularios**: recorrer los cuatro ejercicios de
   ConstraintLayout. Los formularios de correo y entrega validan y guardan
   datos en el dispositivo; el correo es un borrador local.
8. Cerrar sesión y entrar como estudiante. En **Mis cursos y tareas** se ve
   el mismo curso de ejemplo. Mostrar unidades, laboratorio y tarea; desde
   Mis calificaciones se consultan las notas que registre el docente.

Si se quiere mostrar una entrega con archivos, colocar un documento en
Descargas del emulador y seleccionarlo desde la tarea. El docente puede
calificarlo y el estudiante consultar la retroalimentación.

## Qué explicar al profesor

- ConstraintLayout organiza las interfaces; listas y adaptadores muestran resultados.
- SQLite conserva usuarios, cursos y tareas locales; el taller de encomiendas
  demuestra inserción transaccional, búsqueda parametrizada y relaciones mediante JOIN.
- Retrofit usa `@GET`, `@POST` y `@Body`; Gson convierte JSON; las corrutinas
  evitan bloquear la interfaz durante las peticiones.
- La API del servidor delega en EJB y JPA y persiste en Derby.
- Una sola app no significa que el servidor se ejecute dentro del teléfono:
  para demostrar REST el computador debe tener el backend iniciado.

## Compilar en otro equipo

Android: abrir la raíz en Android Studio, SDK 35, JDK 17 o 21; ejecutar
`./gradlew assembleDebug`. El APK se genera en
`app/build/outputs/apk/debug/app-debug.apk`.

Backend: seguir `backend/README.md`, compilar con `mvn -f backend/pom.xml package`
y desplegar el EAR de `backend/academica-ear/target/`. En teléfono físico usar
la IP LAN del computador y conectar ambos a la misma red.

La guía de servicios detallada está en [taller-retrofit.md](taller-retrofit.md).

## Comprobaciones de la integración

- APK y APK de pruebas compilados con `assembleDebug assembleDebugAndroidTest`.
- Seis pruebas Retrofit correctas y `lintDebug` sin errores.
- Once pruebas instrumentadas correctas en Android API 28: nueve del flujo
  académico y dos de encomiendas (persistencia, JOIN, búsqueda parametrizada
  y rollback de cambios de persona ante guía duplicada).
- Backend incluido compilado con `mvn -o -f backend/pom.xml package`.
- `tests/ui_integracion.py` recorre las pantallas reales del APK y guarda
  capturas en `tests/evidencias-integracion/`.

El recorrido visual verificó el menú único, el curso de ejemplo, el GET real,
la consulta y búsqueda de encomiendas, los cuatro ejercicios, la persistencia
del borrador y la vista del estudiante. Las siete capturas están en
`tests/evidencias-integracion/`. El texto del curso se compara sin distinguir
mayúsculas porque Android capitaliza los botones del estudiante.
