"""UI smoke scenario. Creates an explicitly named validation course on the emulator."""
import re, time
import ui_smoke as ui

def tap_prefix(value):
    for n in ui.nodes():
        if n.get('text','').startswith(value):
            x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
            ui.adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.5);return
    raise AssertionError('No se encontró '+value)

def fill_hint(hint,value):
    ui.tap(hint);ui.adb('shell','input','text',value.replace(' ','%s'));ui.back()

if __name__=='__main__':
    ui.launch()
    if 'Iniciar sesión' in ui.texts(): ui.login('docente')
    ui.tap('btnMisCursos','resource-id')
    ui.tap('btn_nuevo_curso','resource-id')
    ui.field('et_nombre_curso','Validacion HU10-HU12')
    ui.field('et_descripcion_curso','Curso de verificacion funcional')
    for field in ['btn_fecha_inicio','btn_fecha_fin']:
        ui.tap(field,'resource-id');ui.tap('android:id/button1','resource-id')
    ui.tap('btn_guardar_curso','resource-id')
    tap_prefix('Validacion HU10-HU12')
    ui.tap('sw_tardias','resource-id')
    assert ui.find('sw_tardias','resource-id').get('checked')=='true'
    ui.tap('btn_laboratorio','resource-id')
    ui.tap('Nueva práctica')
    fill_hint('Título de la práctica','Practica HU12')
    fill_hint('Objetivo','Comprender el flujo experimental')
    fill_hint('Guía: materiales, pasos y resultado esperado','1. Preparar materiales. 2. Medir. 3. Registrar resultados.')
    ui.tap('Publicar práctica');time.sleep(1)
    assert 'Practica HU12' in ui.texts()
    print('HU-11: política activada. HU-12: práctica publicada desde la interfaz.',flush=True)
    ui.back();ui.tap('btn_tareas_curso','resource-id');ui.tap('btn_nueva_tarea','resource-id')
    ui.field('et_titulo_tarea','Informe HU10')
    ui.field('et_enunciado_tarea','Adjuntar resultados de la practica')
    ui.field('et_puntaje_tarea','100')
    ui.tap('btn_fecha_limite_tarea','resource-id');ui.tap('android:id/button1','resource-id')
    ui.tap('btn_guardar_tarea','resource-id')
    for _ in range(3): ui.back()
    ui.tap('btnLogout','resource-id');ui.login('estudiante')
    ui.tap('btnCursosDisponibles','resource-id');tap_prefix('Validacion HU10-HU12');ui.tap('android:id/button1','resource-id')
    tap_prefix('Validacion HU10-HU12');ui.tap('btn_laboratorio','resource-id')
    assert 'Practica HU12' in ui.texts()
    assert 'Nueva práctica' not in ui.texts()
    print('HU-12: estudiante inscrito consulta guía sin controles de creación.',flush=True)
    for _ in range(3): ui.back()
    ui.tap('btnCalificaciones','resource-id')
    text=ui.texts();assert 'Validacion HU10-HU12' in text and 'Informe HU10' in text
    assert any('0 / 100 puntos publicados' in value for value in text)
    print('HU-10: agrupación por curso, tarea y acumulado visibles.',flush=True)
    print('\n'.join(text),flush=True)
