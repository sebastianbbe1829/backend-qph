@echo off
setlocal
title Iniciar QPH - Productos y Ventas

powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0iniciar_prueba_qph.ps1"
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo.
    echo No se pudo iniciar QPH. Revisa el mensaje anterior.
    pause
)

exit /b %EXIT_CODE%
