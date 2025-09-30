@echo off
echo ========================================
echo ESP32 UDP Lab - Serial Monitor
echo ========================================
echo.

REM Verificar si las dependencias están instaladas
python -c "import PyQt6; import serial; import pyqtgraph" >nul 2>&1
if errorlevel 1 (
    echo ❌ Dependencias no instaladas
    echo Ejecuta primero: install_dependencies.bat
    pause
    exit /b 1
)

echo 🚀 Iniciando ESP32 Serial Monitor...
echo.

REM Ejecutar la aplicación
python esp32_serial_monitor.py

if errorlevel 1 (
    echo.
    echo ❌ Error al ejecutar la aplicación
    pause
)