"""Recorre el APK integrado instalado; conserva los ejemplos de presentación."""
import time
import xml.etree.ElementTree as ET
from pathlib import Path
import ui_smoke as ui

_counter = 0
def fresh_nodes():
    global _counter
    for _ in range(4):
        _counter += 1
        path = '/sdcard/cursos-check-' + str(_counter) + '.xml'
        result = ui.adb('shell','uiautomator','dump','--compressed',path)
        if 'dumped to' in result:
            try:
                return list(ET.fromstring(ui.adb('shell','cat',path)).iter('node'))
            except ET.ParseError: pass
        time.sleep(1)
    raise RuntimeError('No se pudo leer la pantalla actual del emulador')
ui.nodes = fresh_nodes
def replace_field(id, value):
    node = ui.find(id, 'resource-id')
    count = len(node.get('text', '')) + 1
    ui.tap(id, 'resource-id')
    ui.adb('shell', 'input', 'keyevent', '123')
    ui.adb('shell', 'input', 'keyevent', *(['67'] * count))
    ui.adb('shell', 'input', 'text', value.replace(' ', '%s'))
    ui.adb('shell', 'input', 'keyevent', '4')
    time.sleep(.3)
ui.field = replace_field
OUT=Path(__file__).resolve().parent/'evidencias-integracion'
OUT.mkdir(exist_ok=True)

def scroll(up=False):
    ui.adb('shell','input','swipe','240','240' if up else '690','240','690' if up else '240','300')
    time.sleep(.25)

def tap_id(id):
    for _ in range(9):
        try:
            ui.tap(id,'resource-id')
            return
        except AssertionError: scroll()
    raise AssertionError('No se encontró '+id)

def top():
    for _ in range(5): scroll(True)

def screenshot(name):
    time.sleep(1.5)
    import subprocess
    with (OUT/(name+'.png')).open('wb') as out:
        subprocess.run([ui.ADB,'exec-out','screencap','-p'],stdout=out,check=True)

def check(text):
    assert any(text.casefold() in t.casefold() for t in ui.texts()), '\n'.join(ui.texts())
    print('OK:',text,flush=True)

def finish_student():
    check('Demo · Desarrollo de aplicaciones móviles')
    screenshot('07-estudiante');ui.back()
    tap_id('btnLogout');ui.login('docente');top()
    print('Recorrido completado. App lista en el menú del docente.',flush=True)

def main():
    ui.adb('shell','input','keyevent','82')
    ui.launch()
    time.sleep(3)
    for _ in range(8):
        visible = ui.nodes()
        if any(n.get('resource-id','').endswith(('/etUsername','/btnMisCursos')) for n in visible): break
        time.sleep(1)
    if any(n.get('resource-id','').endswith('/etUsername') for n in ui.nodes()):
        ui.login('docente')
    else:
        tap_id('btnLogout');ui.login('docente')
    time.sleep(1)
    tap_id('btnDemo');time.sleep(1)
    top();screenshot('01-menu')
    tap_id('btnMisCursos');check('Demo · Desarrollo de aplicaciones móviles');screenshot('02-cursos');ui.back()
    tap_id('btnServiciosRest');tap_id('loadCourses');time.sleep(2);check('Consulta completada');screenshot('03-rest');ui.back()
    tap_id('btnEncomiendas');tap_id('enc_btn_consultar');check('ENC-DEMO-001');screenshot('04-encomiendas')
    ui.field('enc_et_buscar','100100');tap_id('enc_btn_buscar');check('Resultados para');ui.back();ui.back()
    tap_id('btnLayouts');screenshot('05-interfaces')
    for n in range(1,5):
        tap_id('cl_exercise'+str(n));check('Ejercicio '+str(n));ui.back()
    tap_id('cl_email');ui.field('cl_email_address','docente@example.com');ui.field('cl_subject','Presentacion Android');ui.field('cl_message','Integracion de talleres');tap_id('cl_send');ui.back()
    tap_id('cl_email');check('docente@example.com');ui.back()
    tap_id('cl_delivery');screenshot('06-formulario');ui.back();ui.back()
    tap_id('btnLogout');ui.login('estudiante');tap_id('btnMisCursos')
    finish_student()

if __name__ == '__main__':
    import sys
    if '--finish-student' in sys.argv: finish_student()
    else: main()
