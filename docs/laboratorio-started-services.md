# Laboratorio de started services y notificaciones

Implementación de la presentación **Convergentes- started-services.pdf**, de Gilberto Pedraza García, dentro de la app Cursos Virtuales. El ejercicio de mensaje retardado se adapta al proyecto mediante un recordatorio editable y avisos de tareas.

## Ejercicio de la presentación

1. Instalar la app e iniciar sesión con una de las cuentas de demostración del README.
2. Abrir **Avisos y servicios** desde el menú principal.
3. Activar las notificaciones. Desde Android 13 se solicita el permiso de notificaciones. En versiones anteriores el botón abre su configuración del sistema.
4. Escribir un mensaje y pulsar **Notificar después de 10 segundos**.
5. Cambiar a otra aplicación o ir al inicio del dispositivo. Al terminar el procesamiento, el aviso aparece en la barra.
6. Abrir la notificación para volver a los avisos de la cuenta. Los avisos asociados a una tarea incluyen **Abrir tarea**.

En Logcat, filtrar por `StartedServices` para observar `onCreate`, `onHandleIntent`, el hilo de trabajo, la publicación y `onDestroy`. La demostración también escribe su mensaje en el log. La espera se ejecuta en el hilo de `IntentService` y permite seguir usando la interfaz.

## Avisos del proyecto

### Entrega recibida para el docente

1. Como docente, crear un curso y una tarea. La opción **Cargar ejemplos de presentación** prepara una tarea y la inscripción de la cuenta estudiante.
2. Como estudiante, abrir la tarea vigente y entregar uno o varios archivos.
3. La entrega genera un aviso para el docente propietario del curso. El aviso se guarda junto con la entrega en la misma transacción SQLite.
4. Entrar como docente en el mismo dispositivo. El menú principal inicia el servicio y los avisos pendientes aparecen en la barra, si las notificaciones están habilitadas.
5. Abrir el aviso y después la tarea para revisar y calificar las entregas.

### Calificación disponible para el estudiante

1. Como docente, abrir la entrega y guardar una nota válida con retroalimentación.
2. La nota y el aviso para el estudiante se guardan en una misma transacción. Editar la nota genera otro aviso.
3. Entrar como estudiante. El servicio publica sus avisos pendientes y permite abrir la tarea para consultar la nota y el comentario.

La app educativa conserva datos locales y permite alternar los roles en un mismo dispositivo. La comunicación entre teléfonos requiere una integración de servidor y mensajería que queda fuera de este laboratorio. La barra muestra los avisos de la cuenta activa. Al cerrar sesión se retiran de la barra, mientras que el historial se conserva para su destinatario en **Tus avisos**.

## Componentes

- `DelayedMessageService` extiende `IntentService`. Recibe un intent explícito, procesa el trabajo en un hilo de servicio y termina automáticamente al vaciar su cola. Está registrado con `android:exported="false"`.
- `AvisosRepository` guarda el destinatario, tarea, mensaje, instante disponible y estado de publicación en `AVISO`. La migración de la versión 6 a la 7 conserva los datos existentes.
- `AvisosAndroid` crea el canal de actividad académica, el icono y el `PendingIntent` inmutable. Cada aviso tiene un identificador propio para conservar varios eventos. La pantalla bloqueada utiliza una versión pública con texto genérico.
- `AvisosActivity` permite activar notificaciones, iniciar la demostración y consultar los avisos de la cuenta. Al abrir un aviso se vuelve a verificar la sesión y el destinatario.
- `LearningRepository.submit` y `grade` generan avisos únicamente cuando se confirma la operación académica. Una entrega o nota rechazada conserva el estado anterior.

## Comportamiento en Android actual

`IntentService` está obsoleto desde API 30 y se utiliza aquí porque la actividad del curso lo solicita. Las solicitudes se inician desde pantallas visibles y el trabajo es breve. Android puede detener servicios de una app inactiva. Los avisos pendientes permanecen en SQLite y se vuelven a procesar al entrar en la cuenta o pulsar **Actualizar avisos**. [Referencia de IntentService](https://developer.android.com/reference/android/app/IntentService) y [límites de ejecución en segundo plano](https://developer.android.com/about/versions/oreo/background).

Si se deniega el permiso, se desactiva el canal o se bloquean las notificaciones de la app, el aviso permanece en el historial y pendiente de publicación. Se puede habilitar desde la configuración de notificaciones del sistema y regresar a la app. La solicitud del permiso de Android 13 se realiza en **Activar notificaciones**. [Permiso de notificaciones](https://developer.android.com/develop/ui/views/notifications/notification-permission).

## Compilar y probar

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Para ejecutar las pruebas de servicio en una instalación de laboratorio con almacenamiento propio, usar la variante aislada del proyecto.

```bash
./gradlew -Pmapas.isolatedTest=true :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.cursosvirtuales.app.LearningFlowTest,com.cursosvirtuales.app.EncomiendasIntegrationTest,com.cursosvirtuales.app.StartedServiceTest com.cursosvirtuales.app.mapaslab.test/android.test.InstrumentationTestRunner
```

`LearningFlowTest` comprueba el aviso de entrega, las notas válidas/rechazadas, los destinatarios y la migración. `StartedServiceTest` ejecuta el servicio real, abandona la pantalla durante la espera, comprueba la notificación y su `PendingIntent`, y verifica que una cuenta diferente mantenga pendiente el evento. Los tests usan la instalación aislada y datos de ensayo.

La entrega consiste en la URL del repositorio GitHub. Esta guía y el código están incluidos en el mismo proyecto que los otros laboratorios de Android.

## Resultado de la validación

Validado el 10 de noviembre de 2026 en Android 9 (API 28), con la variante de almacenamiento aislado. La compilación terminó correctamente y pasaron 8 pruebas unitarias y 15 instrumentadas. El servicio publicó el recordatorio tras la espera mientras el inicio de Android tenía el foco. Su `PendingIntent` abrió la app. Se verificaron los avisos por entrega y calificación, las operaciones rechazadas, la cuenta destinataria, la retirada de notificaciones al cerrar sesión y la migración de SQLite. La compatibilidad del permiso de Android 13 se implementa mediante la API del sistema; la ejecución instrumentada de esta entrega se realizó en API 28.
