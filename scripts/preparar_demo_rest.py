#!/usr/bin/env python3
"""Añade ejemplos REST si no existen. No elimina ni reemplaza registros."""
import json
import sys
import urllib.request

BASE=(sys.argv[1] if len(sys.argv)>1 else 'http://127.0.0.1:8080/academica/api/').rstrip('/')+'/'
def request(path, data=None):
    payload=None if data is None else json.dumps(data).encode()
    req=urllib.request.Request(BASE+path,data=payload,headers={'Content-Type':'application/json','Accept':'application/json'})
    with urllib.request.urlopen(req,timeout=15) as response:
        return json.load(response)
if not any(c['codigo']=='DEMO-REST-101' for c in request('cursos')):
    request('cursos',dict(codigo='DEMO-REST-101',nombre='Demo - Servicios REST con Retrofit',creditos=3,semestre=2,cupo=20))
students=request('estudiantes')
student=next((e for e in students if e['codigo']=='DEMO-EST-001'),None)
if student is None:
    request('estudiantes',dict(codigo='DEMO-EST-001',nombre='Estudiante de demostracion',correo='demo@example.com'))
    student={'cursos':[]}
if 'DEMO-REST-101' not in student['cursos']:
    request('inscripciones',dict(estudiante='DEMO-EST-001',curso='DEMO-REST-101'))
print('Ejemplos REST listos: DEMO-REST-101 / DEMO-EST-001.')
