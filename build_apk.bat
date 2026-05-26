@echo off
setlocal
chcp 65001 > nul

cd /d "%~dp0"

echo ==========================================
echo       COMPILANDO APK DO FINGUIA
echo ==========================================
echo.

:: Define local gradle home se necessário
set GRADLE_USER_HOME=D:\.gradle

:: Tenta localizar JAVA_HOME se não estiver definido
if "%JAVA_HOME%"=="" (
    if exist "C:\Users\Administrator\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2" (
        set "JAVA_HOME=C:\Users\Administrator\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
    )
)

if not "%JAVA_HOME%"=="" (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

echo Usando JAVA_HOME: %JAVA_HOME%
echo.

call gradlew.bat assembleDebug

if %ERRORLEVEL% equ 0 (
    echo.
    echo ==========================================
    echo [SUCESSO] APK compilado com sucesso!
    echo local: app\build\outputs\apk\debug\app-debug.apk
    echo ==========================================
) else (
    echo.
    echo [ERRO] Falha na compilação do APK.
)
echo.
pause
