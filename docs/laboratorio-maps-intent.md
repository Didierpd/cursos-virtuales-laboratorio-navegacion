# Laboratorio GPS e Intents de Google Maps

La app integra el recorrido de recolección y el de tutoría. Ambos aceptan un origen capturado con el proveedor de ubicación de Android, coordenadas manuales y un origen de ejemplo en Bogotá.

## Ejecutar

Compilar con JDK 17 o 21 y Android SDK 35.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Instalar `app/build/outputs/apk/debug/app-debug.apk` y entrar con las cuentas de demostración del README. Las rutas y asignaciones funcionan con datos locales y pueden usarse sin iniciar el backend de servicios REST.

## Recorrido de encomiendas

1. En el menú principal, abrir **Encomiendas · SQLite**. Registrar una encomienda o, desde la cuenta docente, usar **Cargar ejemplos de presentación** para crear la guía de demostración.
2. Abrir **Ruta diaria de recolección**. Escribir el nombre o código del recolector y seleccionar la fecha.
3. Pulsar **Asignar encomienda para la fecha**. Elegir una guía e ingresar las coordenadas de recogida en la dirección del remitente. Para el ejemplo se puede usar latitud `4.6496`, longitud `-74.0628`. Guardar y repetir para las otras recogidas.
4. Indicar el origen y pulsar **Planear recorrido**. Comparar el orden recomendado con el orden de registro. Al tocar una parada se abre su ubicación.
5. Abrir cada tramo en Google Maps en el orden mostrado. La última parada de un tramo es el origen del siguiente.

Cada guía admite una asignación por fecha. Guardar de nuevo la misma guía y fecha permite corregir las coordenadas o cambiar el recolector. **Quitar asignación** retira la recogida de esa fecha. SQLite conserva las encomiendas existentes al actualizar la base.

El cálculo empieza por la parada más cercana, compara ese orden con el de registro y mejora la secuencia invirtiendo tramos que reduzcan la distancia acumulada. La estimación usa distancias geográficas en línea recta y termina en la última parada. Google Maps calcula el trayecto por calles para el medio seleccionado. Los enlaces tienen como máximo tres puntos intermedios para que los tramos también funcionen en el navegador móvil. [Documentación de Maps URLs](https://developers.google.com/maps/documentation/urls/get-started).

## Rutas de tutoría

1. Entrar en **Rutas de tutoría**. Las tres ubicaciones de ejemplo permiten probar el mapa desde el primer uso. Para usar ubicaciones reales, pulsar **Agregar sede de tutoría** y guardar nombre del docente o sede y coordenadas. Al guardar la primera sede se muestran las ubicaciones propias.
2. Pulsar **Usar mi ubicación GPS** y conceder permiso. También se puede escribir el origen o seleccionar el ejemplo. **Ver origen en Google Maps** permite comprobar el punto.
3. Pulsar **Comparar sedes cercanas**. La lista muestra la sede más próxima por distancia geográfica y las otras opciones.
4. Seleccionar **A pie**, **Automóvil** o **Bicicleta** y abrir la ruta de la sede deseada. Consultar en Maps los tiempos y las alternativas que muestre para ese medio.

Las sedes de ejemplo son puntos de demostración y las sedes guardadas se administran en el dispositivo por cuenta. Para decidir a qué tutoría asistir se debe contar con las ubicaciones y horarios suministrados por los docentes.

## GPS e Intents

La app solicita ubicación al pulsar el botón de GPS. Con permiso preciso intenta el proveedor GPS y puede utilizar el de red si GPS está desactivado. Con permiso aproximado usa el proveedor de red. Ante permiso denegado, proveedor desactivado o falta de señal se mantienen disponibles las coordenadas manuales.

Para mostrar un punto y solicitar indicaciones se usa `Intent.ACTION_VIEW` con una URL de Maps que indica la ubicación o el origen, el destino, el medio de transporte y los puntos intermedios. Se intenta abrir el paquete de Google Maps y, si falta, se entrega la misma URL al navegador disponible. Los enlaces funcionan sin clave API. [Google Maps Intents para Android](https://developer.android.com/guide/components/google-maps-intents).

## Verificación

La compilación y las 8 pruebas unitarias terminaron correctamente. Las 3 pruebas instrumentadas de encomiendas pasaron en Android 9, incluida la migración desde la versión 1, el aislamiento por día/recolector y la persistencia de asignaciones.

Para instalar una copia de prueba con almacenamiento propio cuando el dispositivo tenga otra versión de la app, compilar así.

```bash
./gradlew -Pmapas.isolatedTest=true :app:assembleDebug
```

Esta variante usa el identificador `com.cursosvirtuales.app.mapaslab`. Para una compilación habitual se utiliza el comando de la sección Ejecutar.
