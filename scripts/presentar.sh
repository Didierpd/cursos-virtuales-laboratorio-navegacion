#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
SDK_DIR="${ANDROID_HOME:-$HOME/Android/Sdk}"
GF_DIR="${GLASSFISH_HOME:-$HOME/Aplicaciones/glassfish41/glassfish4}"
JDK8_DIR="${JAVA8_HOME:-$HOME/Aplicaciones/java8}"
AVD_NAME="${CURSOS_AVD:-Cursos_Ligero_API_28}"
ADB="$SDK_DIR/platform-tools/adb"
API_URL="http://127.0.0.1:8080/academica/api/cursos"
api_ready() {
    python3 - "$API_URL" <<'PY'
import json, sys, urllib.request
try:
    with urllib.request.urlopen(sys.argv[1], timeout=3) as r:
        assert isinstance(json.load(r), list)
except Exception:
    sys.exit(1)
PY
}
if ! api_ready; then
    echo "Iniciando servidor académico…"
    test -d "$GF_DIR/glassfish/databases/AcademicaDB" || { echo "Falta configurar AcademicaDB. Consulta backend/README.md."; exit 1; }
    if ! "$JDK8_DIR/bin/java" -cp "$GF_DIR/javadb/lib/*" org.apache.derby.drda.NetworkServerControl ping -h localhost >/dev/null 2>&1; then
        systemd-run --user --collect --unit=cursos-demo-derby \
            "$JDK8_DIR/bin/java" "-Dderby.system.home=$GF_DIR/glassfish/databases" \
            -cp "$GF_DIR/javadb/lib/*" org.apache.derby.drda.NetworkServerControl start -h localhost
    fi
    if ! "$GF_DIR/bin/asadmin" list-domains | grep -q 'domain1 running'; then
        systemd-run --user --collect --unit=cursos-demo-backend "$GF_DIR/bin/asadmin" start-domain --verbose domain1
    fi
    for ((i=0; i<60; i++)); do
        if "$GF_DIR/bin/asadmin" list-applications >/dev/null 2>&1; then break; fi
        sleep 2
    done
    if [[ ! -f "$PROJECT_DIR/backend/academica-ear/target/GestionAcademicaJPA.ear" ]]; then
        (cd "$PROJECT_DIR/backend" && mvn -q package)
    fi
    "$GF_DIR/bin/asadmin" deploy --force=true "$PROJECT_DIR/backend/academica-ear/target/GestionAcademicaJPA.ear"
fi
api_ready || { echo "El backend no respondió. Consulta backend/README.md."; exit 1; }
python3 "$PROJECT_DIR/scripts/preparar_demo_rest.py"
"$ADB" start-server
if ! "$ADB" devices | grep -q '^emulator-.*device$'; then
    echo "Abriendo emulador Android…"
    systemd-run --user --collect --unit=cursos-demo-emulador \
        --setenv="DISPLAY=${DISPLAY:-:0}" --setenv="XAUTHORITY=${XAUTHORITY:-$HOME/.Xauthority}" \
        --setenv=QT_QPA_PLATFORM=xcb \
        "$SDK_DIR/emulator/emulator" -avd "$AVD_NAME" -no-snapshot-load -no-boot-anim -gpu swiftshader_indirect -memory 1536
fi
for ((i=0; i<120; i++)); do
    if [[ "$("$ADB" -e shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; then break; fi
    sleep 2
 done
[[ "$("$ADB" -e shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]] || { echo "El emulador no terminó de iniciar."; exit 1; }
APK_FILE="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "$APK_FILE" ]]; then (cd "$PROJECT_DIR" && ./gradlew assembleDebug); fi
"$ADB" -e install -r "$APK_FILE"
"$ADB" -e shell am start -n com.cursosvirtuales.app/.ui.LoginActivity
printf '\nApp lista. Docente: docente / docente123. Estudiante: estudiante / estudiante123.\n'
printf 'Servicios REST: http://10.0.2.2:8080/academica/api/\n'
