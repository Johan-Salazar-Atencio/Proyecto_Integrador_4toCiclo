@echo off
setlocal enabledelayedexpansion
title Plantia - puente ADB (8080)

rem ---------------------------------------------------------------
rem Mantiene el reenvio "adb reverse tcp:8080 tcp:8080" activo.
rem Ese tunel vive en la sesion ADB del cable USB: al desconectar y
rem reconectar el cable se pierde. Este script lo reaplica solo.
rem El telefono entonces puede usar http://127.0.0.1:8080/
rem ---------------------------------------------------------------

set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"

if not exist "%ADB%" (
    echo [ERROR] No se encontro adb en:
    echo         %ADB%
    echo         Ajusta la variable ADB en este archivo.
    pause
    exit /b 1
)

echo [Plantia] Puente ADB activo para el puerto 8080.
echo [Plantia] Conecta el telefono por USB; se reaplica solo al reconectar.
echo [Plantia] Ctrl+C para salir.
echo.

:loop
"%ADB%" start-server >nul 2>&1

rem %%a = numero de serie, %%b = estado (device / unauthorized / offline)
set "DEV="
for /f "skip=1 tokens=1,2" %%a in ('"%ADB%" devices 2^>nul') do (
    if /i "%%b"=="device" set "DEV=%%a"
)

if defined DEV (
    if not defined CONECTADO (
        set "CONECTADO=1"
        echo [Plantia] Telefono conectado: %%~nx...
        echo [Plantia]   http://127.0.0.1:8080/  (adb reverse)
    )
    rem idempotente: si ya existe el mapeo, no cambia nada
    "%ADB%" -s !DEV! reverse tcp:8080 tcp:8080 >nul 2>&1
) else (
    if defined CONECTADO (
        set "CONECTADO="
        echo [Plantia] Telefono desconectado. Esperando...
    ) else (
        if not defined AVISADO (
            set "AVISADO=1"
            echo [Plantia] Esperando un telefono por USB...
        )
    )
)

timeout /t 2 /nobreak >nul
goto loop
