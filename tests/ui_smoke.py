"""Helpers for a real UI smoke test using the Android SDK's adb (no extra packages)."""
import subprocess, time, re, xml.etree.ElementTree as ET
from pathlib import Path
ADB='/home/didier1/Android/Sdk/platform-tools/adb'
PKG='com.cursosvirtuales.app'
def adb(*args):
    return subprocess.check_output([ADB,*args],text=True)
def nodes():
    adb('shell','uiautomator','dump','/sdcard/cursos-window.xml')
    return list(ET.fromstring(adb('shell','cat','/sdcard/cursos-window.xml')).iter('node'))
def find(value,attr='text'):
    for n in nodes():
        actual=n.get(attr,'')
        if actual == value or (attr=='resource-id' and actual.endswith('/'+value)):
            return n
    raise AssertionError('No se encontró '+value)
def tap(value,attr='text'):
    n=find(value,attr); x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.4)
def field(id,value):
    tap(id,'resource-id');adb('shell','input','text',value.replace(' ','%s'))
    adb('shell','input','keyevent','4');time.sleep(.2)
def back():
    adb('shell','input','keyevent','4');time.sleep(.3)
def texts():
    return [n.get('text') for n in nodes() if n.get('text')]
def login(user):
    field('etUsername',user);field('etPassword',user+'123');tap('btnLogin','resource-id')
def launch():
    adb('shell','am','start','-n',PKG+'/.ui.LoginActivity');time.sleep(1)
if __name__=='__main__':
    launch(); print('\n'.join(texts()))
