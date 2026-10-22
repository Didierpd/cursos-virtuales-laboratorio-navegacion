#!/usr/bin/env python3
"""API real → EJB → JPA. Usa códigos únicos y limpia vía formularios existentes."""
import http.cookiejar
import json
import re
import sys
import uuid
import urllib.request
import urllib.error
import urllib.parse

BASE = (sys.argv[1] if len(sys.argv) > 1 else 'http://localhost:8080/academica').rstrip('/')
client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
checks = 0

def check(ok, label):
    global checks
    assert ok, label
    checks += 1
    print('OK:', label)

def api(path, data=None, raw=None, expected=200):
    body = raw if raw is not None else (None if data is None else json.dumps(data).encode())
    req = urllib.request.Request(BASE + '/api/' + path, data=body,
        headers={'Accept': 'application/json', 'Content-Type': 'application/json'})
    try:
        response = client.open(req, timeout=30)
    except urllib.error.HTTPError as ex:
        response = ex
    with response:
        result = json.load(response)
        check(response.status == expected, path + ': esperado HTTP ' + str(expected) + ', recibido ' + str(response.status) + ' ' + str(result))
        check('application/json' in response.headers.get('Content-Type', ''), 'Respuesta JSON')
        return result

prefix = 'R' + uuid.uuid4().hex[:8].upper()
e, e2, c = prefix+'E', prefix+'E2', prefix+'C'
with client.open(BASE + '/gestion', timeout=30) as response:
    csrf = re.search(r'name="csrf" value="([^"]+)"', response.read().decode()).group(1)

def form(action, **fields):
    payload = urllib.parse.urlencode(dict(accion=action, csrf=csrf, **fields)).encode()
    try:
        with client.open(BASE + '/gestion', payload, timeout=30) as response:
            return response.status
    except urllib.error.HTTPError as ex:
        ex.close()
        return ex.code

try:
    check(isinstance(api('cursos'), list), 'GET cursos devuelve lista')
    check(isinstance(api('estudiantes'), list), 'GET estudiantes devuelve lista')
    for code in (e, e2):
        api('estudiantes', dict(codigo=code, nombre='Estudiante REST', correo='rest@example.com'), expected=201)
    curso = dict(codigo=c, nombre='Curso REST', creditos=3, semestre=2, cupo=1)
    api('cursos', curso, expected=201)
    check(any(x['codigo'] == c and x['cupo'] == 1 for x in api('cursos')), 'Curso persistido')
    api('cursos', curso, expected=400)
    api('cursos', dict(curso, codigo=prefix+'BAD', creditos=0), expected=400)
    api('cursos', dict(curso, creditos=1.5), expected=400)
    api('cursos', {}, expected=400)
    api('cursos', raw=b'{malformed', expected=400)
    api('inscripciones', dict(estudiante=e, curso=c), expected=201)
    api('inscripciones', dict(estudiante=e, curso=c), expected=400)
    api('inscripciones', dict(estudiante=e2, curso=c), expected=400)
    check(any(x['codigo'] == e and c in x['cursos'] for x in api('estudiantes')), 'Relación persistida en GET estudiantes')
    check(any(x['codigo'] == c and x['inscritos'] == 1 for x in api('cursos')), 'Cupo refleja la inscripción')
finally:
    form('retirar', estudiante=e, curso=c)
    for code in (e, e2):
        form('eliminarEstudiante', codigo=code)
    form('eliminarCurso', codigo=c)
check(not any(x['codigo'].startswith(prefix) for x in api('cursos')), 'Cursos de prueba eliminados')
check(not any(x['codigo'].startswith(prefix) for x in api('estudiantes')), 'Estudiantes de prueba eliminados')
print(str(checks) + ' comprobaciones correctas.')
