@echo off
REM Copia o banco do FinGuia do aparelho para web\dados\ e consolida o WAL.
REM Precisa de: depuracao USB ligada, adb no PATH e build debug instalada
REM (o comando run-as so funciona em apps depuraveis).
setlocal
set PACOTE=com.finguia
set DESTINO=%~dp0dados

where adb >nul 2>&1
if errorlevel 1 (
    echo [ERRO] adb nao encontrado no PATH.
    echo Adicione a pasta platform-tools do Android SDK ao PATH.
    pause
    exit /b 1
)

adb get-state >nul 2>&1
if errorlevel 1 (
    echo [ERRO] Nenhum aparelho conectado. Confira o cabo e a depuracao USB.
    pause
    exit /b 1
)

if not exist "%DESTINO%" mkdir "%DESTINO%"

echo Copiando banco de %PACOTE% ...
for %%A in (finguia_database finguia_database-wal finguia_database-shm) do (
    adb exec-out run-as %PACOTE% cat databases/%%A > "%DESTINO%\%%A" 2>nul
    REM Arquivo vazio = nao existe no aparelho (o -wal some depois do checkpoint)
    for %%B in ("%DESTINO%\%%A") do if %%~zB EQU 0 del "%DESTINO%\%%A"
)

if not exist "%DESTINO%\finguia_database" (
    echo [ERRO] Nao foi possivel ler o banco.
    echo Verifique se a build instalada e a debug ^(run-as so funciona nela^).
    pause
    exit /b 1
)

echo Consolidando o WAL ...
node "%~dp0consolidar-wal.js" "%DESTINO%\finguia_database"

echo.
echo Pronto. Rode finguia-web.bat para abrir no navegador.
pause
endlocal
