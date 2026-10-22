#!/usr/bin/env python3
"""Prueba real JSP → Servlet → EJB → JPA. Crea datos únicos y los limpia."""
import http.cookiejar
import re
import sys
import uuid
import urllib.error
import urllib.parse
import urllib.request

BASE = (sys.argv[1] if len(sys.argv) > 1 else 'http://localhost:8080/academica').rstrip('/')
client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
checks = 0

def request(path='/gestion', data=None):
    payload = None if data is None else urllib.parse.urlencode(data).encode()
    try:
        response = client.open(BASE + path, payload, timeout=30)
    except urllib.error.HTTPError as error:
        response = error
    return response.status, response.read().decode(response.headers.get_content_charset() or 'utf-8', errors='replace')

def check(ok, label):
    global checks
    assert ok, label
    checks += 1
    print('OK:', label)

status, page = request()
check(status == 200, 'Página y JSP disponibles')
csrf = re.search(r'name="csrf" value="([^"]+)"', page).group(1)
prefix = 'T' + uuid.uuid4().hex[:8].upper()
e1, e2, c1, c2 = [prefix + suffix for suffix in ('E1', 'E2', 'C1', 'C2')]

def post(action, expected=200, text=None, **fields):
    status, page = request(data=dict(accion=action, csrf=csrf, **fields))
    check(status == expected and (text is None or text in page), action + ' → ' + str(expected))
    return page

try:
    check(request(data=dict(accion='eliminarCurso', codigo=c1, csrf='invalido'))[0] == 403, 'Rechaza formulario sin token válido')
    for code in (e1, e2):
        post('guardarEstudiante', codigo=code, nombre='Estudiante <prueba>', correo='prueba@example.com', text='&lt;prueba&gt;')
    post('guardarEstudiante', 400, 'Ya existe', codigo=e1, nombre='Duplicado', correo='prueba@example.com')
    for code, capacity in ((c1, 2), (c2, 1)):
        post('guardarCurso', codigo=code, nombre='Curso de prueba', creditos=3, semestre=2, cupo=capacity)
    post('guardarCurso', 400, codigo=prefix+'BAD', nombre='Inválido', creditos=0, semestre=1, cupo=2)
    post('inscribir', estudiante=e1, curso=c1)
    post('inscribir', estudiante=e1, curso=c2)
    page = post('inscribir', estudiante=e2, curso=c1)
    for student, course in ((e1,c1), (e1,c2), (e2,c1)):
        check('data-inscripcion="'+student+':'+course+'"' in page, 'Relación persistida '+student+':'+course)
    post('inscribir', 400, 'ya está inscrito', estudiante=e1, curso=c1)
    post('inscribir', 400, 'no tiene cupos', estudiante=e2, curso=c2)
    post('guardarCurso', 400, 'no puede ser menor', codigo=c1, nombre='Curso', creditos=3, semestre=2, cupo=1, editar='true')
    post('eliminarEstudiante', 400, estudiante=e1, codigo=e1)
    post('eliminarCurso', 400, codigo=c1)
    post('guardarEstudiante', codigo=e1, nombre='Nombre actualizado', correo='nuevo@example.com', editar='true', text='Nombre actualizado')
    post('guardarCurso', codigo=c1, nombre='Curso actualizado', creditos=4, semestre=3, cupo=3, editar='true', text='Curso actualizado')
    # Una sesión nueva debe recuperar lo escrito desde la base de datos.
    client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    status, page = request()
    check(status == 200 and 'data-inscripcion="'+e1+':'+c2+'"' in page, 'Inscripción disponible en una sesión nueva')
    csrf = re.search(r'name="csrf" value="([^"]+)"', page).group(1)
    check(request('/WEB-INF/studentInfo.jsp')[0] == 404, 'JSP protegida del acceso directo')
    check(request('/assets/academica.css')[0] == 200, 'Estilos disponibles')
finally:
    for student, course in ((e1,c1), (e1,c2), (e2,c1)):
        request(data=dict(accion='retirar', csrf=csrf, estudiante=student, curso=course))
    for action, codes in (('eliminarEstudiante',(e1,e2)), ('eliminarCurso',(c1,c2))):
        for code in codes:
            request(data=dict(accion=action, csrf=csrf, codigo=code))
status, page = request()
check(status == 200 and prefix not in page, 'Retiro, eliminación y limpieza de datos de prueba')
print(str(checks) + ' comprobaciones correctas.')
