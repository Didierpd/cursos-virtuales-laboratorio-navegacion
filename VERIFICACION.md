# Verificación — 30 de septiembre de 2026

- Compilación de aplicación, APK de pruebas y Android Lint: correcta con los ajustes finales.
- Las nuevas fuentes para HU-10 a HU-15 están integradas. La compilación final
  se ejecutó con `assembleDebug assembleDebugAndroidTest lintDebug`.
- Pruebas instrumentadas anteriores: OK (4 tests), ejecutadas en Android 9 / API 28.
- Las pruebas ampliadas de HU-10 a HU-15 están preparadas en
  `app/src/androidTest/java/com/cursosvirtuales/app/LearningFlowTest.java`, pero
  no se ejecutaron en esta última sesión porque el emulador quedó offline al
  reiniciarse.
- Se comprobaron entregas con dos archivos, calificación y edición,
  persistencia, permisos, vencimiento, validaciones, orden de unidades
  y migración de base de datos desde la versión 3.
- Se corrigió la compatibilidad de SQLiteOpenHelper con Closeable para
  Android anterior a API 29 y la autorización antes de la transacción.
- La base de datos quedó en versión 6, con política de tardías, laboratorios,
  recursos y diferencia de tiempo de cada entrega.
- Instalación y apertura de LoginActivity en el emulador: correctas.
- Android Lint previo: 0 errores y 35 advertencias; no repetido tras los
  últimos dos ajustes de compatibilidad y bloqueo.
- El recorrido visual completo de todas las pantallas no está automatizado.

Emulador configurado: Cursos_Ligero_API_28, Android 9 AOSP x86_64,
1536 MB de RAM, 2 núcleos y pantalla 480 x 800. Se retiró el AVD anterior
Aula_Pixel_2_API_35 y su imagen Android 15 Google APIs.

La aplicación almacena datos localmente en el mismo dispositivo.
