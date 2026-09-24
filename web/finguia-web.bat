@echo off
REM Executavel do FinGuia Web: sobe o servidor local e abre o navegador.
REM Uso:  finguia-web.bat                  (procura o banco em web\dados\)
REM       finguia-web.bat caminho\do\banco (usa o banco informado)
setlocal

where node >nul 2>&1
if errorlevel 1 (
    echo [ERRO] Node.js nao encontrado no PATH.
    echo Instale em https://nodejs.org e rode este arquivo de novo.
    pause
    exit /b 1
)

cd /d "%~dp0"

if "%~1"=="" (
    node servidor.js
) else (
    node servidor.js --banco "%~1"
)

endlocal
