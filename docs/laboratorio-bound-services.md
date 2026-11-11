# Laboratorio de bound services y odómetro de tutorías

Basado en las presentaciones **Convergentes-bound-services-Parte 1.pdf** y **Convergentes-bound-services-Parte 2.pdf**, de Gilberto Pedraza García. La implementación utiliza un servicio `Service`, un `Binder` local y posiciones del dispositivo para registrar metros recorridos por el estudiante.

## Demostración en la app

1. Iniciar sesión como estudiante. La cuenta de demostración es `estudiante` con contraseña `estudiante123`.
2. Abrir **Odómetro de tutoría** desde el menú principal. También se puede abrir desde una sede en **Rutas de tutoría**, con la opción **Registrar desplazamiento**.
3. Escribir el proceso formativo o la tutoría. Por ejemplo, `Tutoría de Matemáticas`.
4. Introducir la **precisión del movimiento en metros** y el **tiempo de actualización en segundos**. Los valores iniciales son 5 m y 5 s.
5. Pulsar **Obtener ubicación actual** y conceder el permiso de ubicación. La interfaz presenta latitud, longitud y precisión GPS aproximada. Si el proveedor está desactivado, usar **Ajustes de ubicación**.
6. Pulsar **Iniciar / Reanudar recorrido** y desplazarse manteniendo visible la pantalla del odómetro. Los metros se actualizan en pantalla.
7. **Pausar recorrido** guarda el avance. **Finalizar y guardar recorrido** cierra la sesión de desplazamiento y permite iniciar otra tutoría o configurar otros valores.
8. Pulsar **Reporte consolidado** para consultar el total acumulado, los totales por proceso formativo y el historial de recorridos.

Al salir de la pantalla, se guarda y pausa el recorrido y se libera el enlace. Al regresar, se conserva el acumulado y se continúa mediante **Reanudar**. La primera ubicación nueva de cada reanudación sirve como origen del siguiente tramo, para evitar sumar un desplazamiento ocurrido durante una pausa. La configuración se mantiene durante el recorrido; para cambiarla se finaliza el registro y se inicia uno nuevo.

## Precisión, intervalo y cálculo

La precisión del movimiento se interpreta como la distancia mínima entre un punto de referencia aceptado y una nueva posición para sumar metros. Se entrega a `LocationManager.requestLocationUpdates` como `minDistance`. El intervalo se captura en segundos y se convierte a milisegundos para `minTime`.

El servicio utiliza `Location.distanceTo` para calcular cada tramo. La primera posición establece el origen con 0 metros. El movimiento se suma cuando supera el umbral configurado y la precisión informada por las lecturas GPS, reduciendo el efecto del ruido. Las lecturas anteriores al inicio del segmento, fuera de orden o recibidas antes del intervalo configurado se descartan.

El intervalo es una solicitud al proveedor. La recepción depende de permisos, señal y estado del dispositivo. La distancia es una estimación de la trayectoria observada entre muestras; intervalos amplios y señal imprecisa pueden omitir curvas y movimientos cortos. La ubicación aproximada puede utilizar el proveedor de red y presentar menor precisión. El reporte conserva la marca de ubicación simulada cuando el sistema la informa.

## Servicio enlazado

`OdometerService` implementa `onBind` y devuelve `OdometerBinder`. `OdometerActivity` usa `ServiceConnection` y `BIND_AUTO_CREATE` en `onStart`, consulta el estado y el acumulado y libera el enlace en `onStop`. La presentación de metros se refresca cada segundo. El cálculo y las escrituras SQLite se procesan en un `HandlerThread`, evitando bloquear la interfaz.

`onUnbind` retira las actualizaciones y guarda la pausa. `onDestroy` termina el hilo después de procesar las operaciones pendientes. La implementación sigue el ciclo de enlace de las presentaciones. El registro se realiza mientras la actividad está visible; el historial permite continuar después de salir de ella o reiniciar la app. [Servicios enlazados de Android](https://developer.android.com/develop/background-work/services/bound-services).

La ubicación se obtiene con el permiso disponible y un proveedor activo, priorizando GPS con permiso preciso y usando red cuando corresponde. Se puede solicitar una lectura puntual desde la interfaz, antes de iniciar el recorrido. [Referencia de LocationManager](https://developer.android.com/reference/android/location/LocationManager).

## Persistencia y reporte

La base local migra de la versión 7 a la 8 conservando los datos del proyecto.

- `DESPLAZAMIENTO` guarda estudiante, proceso formativo, inicio, fin, estado, configuración, metros acumulados, última posición, número de muestras e indicación de simulación.
- `PUNTO_RECORRIDO` conserva fecha, coordenadas, precisión GPS, metros del tramo y la indicación de ubicación simulada de cada muestra recibida y aceptada para registro.
- La muestra y el acumulado se confirman en una transacción. Un identificador de segmento evita que callbacks pendientes de un enlace anterior modifiquen un recorrido ya reanudado.
- El reporte consulta únicamente los registros del estudiante activo, agrupa por proceso formativo y muestra también el historial. Los recorridos pausados conservan los metros observados hasta la pausa.

Los datos se guardan en el dispositivo de la app, igual que las entregas y calificaciones locales del proyecto.

## Compilar

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Para usar una instalación de laboratorio con almacenamiento propio, compilar la variante aislada.

```bash
./gradlew -Pmapas.isolatedTest=true :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.cursosvirtuales.app.OdometerServiceTest com.cursosvirtuales.app.mapaslab.test/android.test.InstrumentationTestRunner
```

La prueba utiliza el `Binder` real y un proveedor GPS de prueba del sistema. Envía posiciones conocidas y verifica la distancia, la persistencia, la pausa al salir, la reanudación sin sumar el salto entre segmentos, la finalización y la privacidad del reporte. El simulador se retira al terminar y los datos de ensayo se mantienen separados de la instalación habitual.

La entrega es la URL del repositorio GitHub, que incluye el código y esta guía.

## Resultado de validación

El 10 de noviembre de 2026 se compiló la app y pasaron 8 pruebas unitarias y 17 instrumentadas en Android 9 (API 28), usando la variante aislada. La prueba del odómetro recorrió dos segmentos con coordenadas GPS simuladas conocidas, verificó el acumulado después de liberar y volver a enlazar el servicio, y comprobó que el cambio de posición durante una pausa no se sumara al total. También validó el registro finalizado, la marca de simulación y el consolidado por proceso formativo. La migración desde la versión 7 conservó los avisos y creó las tablas del odómetro. Las pruebas académicas, de encomiendas y del servicio iniciado anterior continuaron pasando.
