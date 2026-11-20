@echo off
REM Maven "portable": si no hay Maven instalado, lo baja una sola vez a %USERPROFILE%\.m2\wrapper
REM Ademas arregla JAVA_HOME si no esta o apunta a una carpeta sin Java.
setlocal
set "MVN_VER=3.9.9"
set "MVN_BASE=%USERPROFILE%\.m2\wrapper"
set "MVN_DIR=%MVN_BASE%\apache-maven-%MVN_VER%"

REM ---------- Buscar Java ----------
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto javaok
set "JAVA_HOME="

REM 1) El java que este en el PATH (pregunta su java.home real)
set "JH="
for /f "tokens=2 delims==" %%a in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /c:"java.home"') do set "JH=%%a"
if defined JH set "JAVA_HOME=%JH:~1%"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto javaok
set "JAVA_HOME="

REM 2) Carpetas tipicas de JDK (incluye las que baja IntelliJ en .jdks)
for %%d in ("%USERPROFILE%\.jdks" "%ProgramFiles%\Java" "%ProgramFiles%\Eclipse Adoptium" "%ProgramFiles%\Microsoft" "%ProgramFiles%\Zulu" "%ProgramFiles%\Amazon Corretto" "%ProgramFiles%\BellSoft") do (
  if exist "%%~d" for /d %%j in ("%%~d\*") do if exist "%%~j\bin\java.exe" set "JAVA_HOME=%%~j"
)
if defined JAVA_HOME goto javaok

echo [ERROR] No encuentro Java. Instala Java 17 o superior, por ejemplo Temurin 17, y volve a probar.
exit /b 1

:javaok
echo Usando Java en: %JAVA_HOME%
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM ---------- Maven ----------
if exist "%MVN_DIR%\bin\mvn.cmd" goto run
echo Descargando Maven %MVN_VER%, solo la primera vez...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol='Tls12'; $z=Join-Path $env:TEMP 'apache-maven.zip'; Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVN_VER%/apache-maven-%MVN_VER%-bin.zip' -OutFile $z -UseBasicParsing; New-Item -ItemType Directory -Force '%MVN_BASE%' | Out-Null; Expand-Archive $z -DestinationPath '%MVN_BASE%' -Force; Remove-Item $z"
if errorlevel 1 (
  echo [ERROR] No se pudo descargar Maven. Revisa tu conexion a internet.
  exit /b 1
)

:run
call "%MVN_DIR%\bin\mvn.cmd" %*
