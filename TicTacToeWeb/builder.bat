@echo off
setlocal enabledelayedexpansion

REM ==============================
REM === JAVA SETTINGS ===========
REM ==============================
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "JAR=%JAVA_HOME%\bin\jar.exe"
set "JPACKAGE=%JAVA_HOME%\bin\jpackage.exe"
set "JLINK=%JAVA_HOME%\bin\jlink.exe"

REM ==============================
REM === WiX SETTINGS (unchanged)
REM ==============================
set "WIX_HOME=C:\WiX\bin"
set PATH=%WIX_HOME%;%PATH%

REM ==============================
REM === PROJECT SETTINGS ========
REM ==============================
set "SRC_DIR=C:\Games\TicTacToeWeb\src"
set "OUT_DIR=C:\Games\TicTacToeWeb\out"
set "BUILD_DIR=C:\Games\TicTacToeWeb\build"
set "DIST_DIR=C:\Games\TicTacToeWeb\dist"
set "PORTABLE_DIR=%DIST_DIR%\TicTacToeWebApp"
set "RUNTIME_IMAGE=%BUILD_DIR%\runtime"
set "JAR_NAME=TicTacToeWeb.jar"
set "MAIN_CLASS=frontend.HttpServerApp"

REM External libraries (paths to JARs)
set "JSON_LIB=C:\json-20250517.jar"
set "SQLITE_JDBC=C:\sqlite-jdbc-3.50.3.0.jar"

REM Resources to copy (includes the DB file and the jars)
set "RESOURCES=index.html icon.ico tictactoe.db %JSON_LIB% %SQLITE_JDBC%"

REM ==============================
REM === CLEAN OUTPUT FOLDERS =====
REM ==============================
if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"
if exist "%PORTABLE_DIR%" rmdir /s /q "%PORTABLE_DIR%"
mkdir "%PORTABLE_DIR%"
if exist "%RUNTIME_IMAGE%" (
    echo Removing old runtime...
    cmd /c rd /s /q "%RUNTIME_IMAGE%" >nul 2>&1
    powershell -NoProfile -Command "if (Test-Path '%RUNTIME_IMAGE%') { Remove-Item -LiteralPath '%RUNTIME_IMAGE%' -Recurse -Force -ErrorAction SilentlyContinue }"
    timeout /t 1 >nul
)
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

REM ==============================
REM === COLLECT / COMPILE ========
REM ==============================
echo Compiling Java source files...

REM Build list of java files and compile using javac @filelist approach
set "JAVA_LIST=%BUILD_DIR%\java_files.txt"
if exist "%JAVA_LIST%" del /f /q "%JAVA_LIST%"
for /r "%SRC_DIR%" %%F in (*.java) do echo %%F>>"%JAVA_LIST%"

"%JAVAC%" -cp "%JSON_LIB%;%SQLITE_JDBC%" -d "%OUT_DIR%" @"%JAVA_LIST%"
if errorlevel 1 (
  echo ❌ Compilation failed!
  pause
  exit /b
)
echo ✅ Compilation successful.

REM ==============================
REM === CREATE MANIFEST ==========
REM ==============================
echo Main-Class: %MAIN_CLASS%> manifest.txt
echo Class-Path: json-20250517.jar sqlite-jdbc-3.50.3.0.jar>> manifest.txt
echo.>> manifest.txt

REM ==============================
REM === PACKAGE INTO RUNNABLE JAR =
REM ==============================
echo Creating runnable JAR...
"%JAR%" cfm "%PORTABLE_DIR%\%JAR_NAME%" manifest.txt -C "%OUT_DIR%" .
if errorlevel 1 (
  echo ❌ JAR creation failed!
  pause
  exit /b
)
echo ✅ JAR created: %PORTABLE_DIR%\%JAR_NAME%

REM ==============================
REM === COPY STATIC RESOURCES ====
REM ==============================
echo Copying static files and DB...
for %%f in (%RESOURCES%) do (
  if exist "%%f" (
    copy /Y "%%f" "%PORTABLE_DIR%\" >nul
    echo ✅ Copied %%~nxF
  ) else (
    echo ⚠️ Resource not found: %%f
  )
)
echo.

REM ==============================
REM === CREATE RUNTIME (jlink) ===
REM ==============================
echo Creating custom Java runtime...
"%JLINK%" ^
  --module-path "%JAVA_HOME%\jmods" ^
  --add-modules java.base,java.desktop,java.logging,jdk.httpserver,java.sql,java.naming ^
  --output "%RUNTIME_IMAGE%" ^
  --strip-debug ^
  --compress 2 ^
  --no-header-files ^
  --no-man-pages
if errorlevel 1 (
  echo ❌ Runtime image creation failed!
  pause
  exit /b
)
if not exist "%RUNTIME_IMAGE%\bin\java.exe" (
  echo ❌ runtime image missing java.exe!
  pause
  exit /b
)
echo ✅ Runtime created at %RUNTIME_IMAGE%
echo.


REM ==============================
REM === CREATE PORTABLE APP IMAGE =
REM ==============================
echo Creating portable app-image (self-contained app)...
if exist "%DIST_DIR%\TicTacToeWeb" rmdir /s /q "%DIST_DIR%\TicTacToeWeb"

"%JPACKAGE%" ^
  --type app-image ^
  --input "%PORTABLE_DIR%" ^
  --main-jar "%JAR_NAME%" ^
  --main-class %MAIN_CLASS% ^
  --name "TicTacToeWeb" ^
  --runtime-image "%RUNTIME_IMAGE%" ^
  --icon "%PORTABLE_DIR%\icon.ico" ^
  --dest "%DIST_DIR%" ^
  --win-console

if errorlevel 1 (
  echo ❌ app-image creation failed!
  pause
  exit /b
)

REM make the output folder name consistent
if exist "%DIST_DIR%\TicTacToeWebApp" rmdir /s /q "%DIST_DIR%\TicTacToeWebApp"
move "%DIST_DIR%\TicTacToeWeb" "%DIST_DIR%\TicTacToeWebApp" >nul 2>&1

echo ✅ Portable app created: %DIST_DIR%\TicTacToeWebApp

REM ==============================
REM === CREATE LAUNCHER BATCH ====
REM ==============================
REM create run.bat that starts the EXE and keeps console visible (helpful if user double-clicks)
(
  echo @echo off
  echo cd /d "%%~dp0"
  echo echo Launching TicTacToeWeb.exe...
  echo "%%~dp0TicTacToeWeb.exe"
  echo echo.
  echo pause
) > "%DIST_DIR%\TicTacToeWebApp\run.bat"

echo ✅ Launcher created: %DIST_DIR%\TicTacToeWebApp\run.bat
echo.

REM ==============================
REM === CLEANUP ====================
REM ==============================
del manifest.txt 2>nul
if exist "%JAVA_LIST%" del /f /q "%JAVA_LIST%"

echo Build complete.
echo Portable EXE: %DIST_DIR%\TicTacToeWebApp\TicTacToeWeb.exe
echo Use run.bat in the same folder to launch and keep console visible.
echo.
echo 🌐 Launching TicTacToeWeb locally...
start "" "%DIST_DIR%\TicTacToeWebApp\TicTacToeWeb.exe"
timeout /t 3 >nul
start "" "http://localhost:8000/TicTacToe"
pause

