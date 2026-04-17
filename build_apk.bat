@echo off
set GRADLE_USER_HOME=D:\.gradle
set JAVA_HOME=C:\Users\Administrator\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "D:\PROJETOS\PROJETOS REAIS\FinGuia"
call gradlew.bat assembleDebug
