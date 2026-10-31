@echo off
setlocal
rem Anuncia el backend de Plantia en la red local (mDNS) para que la app
rem Android lo encuentre solo, sin hardcodear la IP.
rem Requisitos: python -m pip install -r scripts\requirements-mdns.txt

set "REPO=%~dp0.."
set "PY=%REPO%\backend-django\.venv\Scripts\python.exe"

if not exist "%PY%" (
    echo [ERROR] No se encontro el interprete de Python en:
    echo         %PY%
    echo         Crea el entorno con: cd backend-django ^&^& py -m venv .venv
    pause
    exit /b 1
)

"%PY%" -c "import zeroconf" >nul 2>&1
if errorlevel 1 (
    echo [INFO] Instalando la dependencia mDNS...
    "%PY%" -m pip install -r "%REPO%\scripts\requirements-mdns.txt"
)

echo [Plantia] Anunciando el backend en la red local. Ctrl+C para detener.
"%PY%" "%REPO%\scripts\anunciar-plantia.py" %*
