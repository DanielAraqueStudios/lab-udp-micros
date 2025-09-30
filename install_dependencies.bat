@echo off
echo ========================================
echo ESP32 UDP Lab - Serial Monitor Setup
echo ========================================
echo.

REM Verificar si Python está instalado
python --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Python no está instalado o no está en el PATH
    echo Por favor instala Python desde https://python.org
    pause
    exit /b 1
)

echo ✅ Python encontrado
echo.

REM Verificar si pip está disponible
pip --version >nul 2>&1
if errorlevel 1 (
    echo ❌ pip no está disponible
    pause
    exit /b 1
)

echo ✅ pip encontrado
echo.

echo 📦 Instalando dependencias...
echo.

REM Instalar dependencias
pip install -r requirements.txt

if errorlevel 1 (
    echo.
    echo ❌ Error al instalar dependencias
    echo Intenta ejecutar: pip install PyQt6 pyserial pyqtgraph
    pause
    exit /b 1
)

echo.
echo ✅ Instalación completada exitosamente!
echo.
echo 🚀 Para ejecutar la aplicación:
echo    python esp32_serial_monitor.py
echo.
pause