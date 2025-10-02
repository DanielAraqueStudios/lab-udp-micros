@echo off
echo ESP32 UDP Controller - Android Build Helper
echo ==========================================
echo.

echo This script helps resolve Android build issues by:
echo 1. Cleaning Gradle cache
echo 2. Resetting Android project state
echo 3. Preparing for Android Studio import
echo.

set PROJECT_DIR=%~dp0android-app

echo Cleaning Gradle cache...
if exist "%PROJECT_DIR%\.gradle" (
    rmdir /s /q "%PROJECT_DIR%\.gradle"
    echo ✓ Cleaned .gradle directory
)

if exist "%PROJECT_DIR%\build" (
    rmdir /s /q "%PROJECT_DIR%\build"
    echo ✓ Cleaned build directory
)

if exist "%PROJECT_DIR%\app\build" (
    rmdir /s /q "%PROJECT_DIR%\app\build"
    echo ✓ Cleaned app/build directory
)

echo.
echo ==========================================
echo ✓ Android project cleaned successfully!
echo.
echo NEXT STEPS:
echo 1. Open Android Studio
echo 2. Click "Open an existing project"
echo 3. Navigate to: %PROJECT_DIR%
echo 4. Let Android Studio sync the project
echo 5. Build using Android Studio GUI
echo.
echo The project is now ready for Android Studio import.
echo ==========================================
pause