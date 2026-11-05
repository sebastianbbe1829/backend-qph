@echo off
setlocal
title Inicializar administrador QPH

powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0bootstrap_admin_qph.ps1"
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo.
    echo No se pudo inicializar el administrador. Revisa el mensaje anterior.
    pause
)

exit /b %EXIT_CODE%
