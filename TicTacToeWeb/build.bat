@echo off
setlocal

REM ============================================================
REM 🚀 Build Script for TicTacToeWeb v1.0 (with embedded runtime)
REM ============================================================

REM === Configuration ===
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "APP_NAME=TicTacToeWeb"
set "APP_VERSION=1.0"
set "MAIN_CLASS=frontend.HttpServerApp"
set "SRC_DIR=C:\Games\TicTacToeWeb\src"
set "OUT_DIR=C:\Games\TicTacToeWeb\out"
set "DIST_DIR=C:\Games\TicTacToeWeb\dist"
set "ICON_PATH=C:\Games\TicTacToeWeb\icon.ico"
set "JSON_LIB=C:\json-20250517.jar"
set "SQLITE_JDBC=C:\sqlite-jdbc-3.50.3.0.jar"
set "JAVAFX_LIB=C:\javafx-sdk-17.0.16\lib"
set "MAIN_JAR=%DIST_DIR%\%APP_NAME%.jar"
set "RUNTIME_IMAGE=%DIST_DIR%\runtime"

echo.
echo ============================================================
echo ⚙️  Building %APP_NAME% v%APP_VERSION%
echo ============================================================

REM === Clean old build ===
if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%OUT_DIR%"
mkdir "%DIST_DIR%"

REM === Step 1: Compile sources ===
echo.
echo 🧩 Compiling Java sources...
dir /S /B "%SRC_DIR%\*.java" > "%OUT_DIR%\sources.txt"

"%JAVA_HOME%\bin\javac.exe" -cp "%JSON_LIB%;%JAVAFX_LIB%\*" -d "%OUT_DIR%" @"%OUT_DIR%\sources.txt"
if errorlevel 1 (
    echo ❌ Compilation FAILED!
    pause
    exit /b
)

REM === Step 2: Package into JAR ===
echo.
echo 📦 Creating executable JAR file...
"%JAVA_HOME%\bin\jar.exe" cfe "%MAIN_JAR%" %MAIN_CLASS% -C "%OUT_DIR%" .

if errorlevel 1 (
    echo ❌ Creating JAR file failed!
    pause
    exit /b
)

REM === Step 3: Copy resources ===
echo.
echo 🧰 Copying index.html, icon, and JSON library...
copy "%JSON_LIB%" "%DIST_DIR%" >nul
copy "%ICON_PATH%" "%DIST_DIR%" >nul
copy "%SQLITE_JDBC%" "%DIST_DIR%" >nul
copy "C:\Games\TicTacToeWeb\index.html" "%DIST_DIR%" >nul

REM === Step 4: Create custom runtime image ===
echo.
echo 🧬 Creating custom Java runtime image...
"%JAVA_HOME%\bin\jlink.exe" ^
--module-path "%JAVA_HOME%\jmods;%JAVAFX_LIB%" ^
--add-modules java.base,java.sql,java.desktop,javafx.controls,javafx.fxml,javafx.web ^
--output "%RUNTIME_IMAGE%" ^
--strip-debug --compress=2 --no-header-files --no-man-pages

if errorlevel 1 (
    echo ❌ Runtime image creation failed!
    pause
    exit /b
)

REM === Step 5: Package app with embedded runtime ===
echo.
echo 🧱 Creating self-contained portable app...
"%JAVA_HOME%\bin\jpackage.exe" ^
--type app-image ^
--input "%DIST_DIR%" ^
--main-jar "%APP_NAME%.jar" ^
--main-class %MAIN_CLASS% ^
--name "%APP_NAME%" ^
--app-version %APP_VERSION% ^
--icon "%ICON_PATH%" ^
--runtime-image "%RUNTIME_IMAGE%" ^
--java-options "--add-modules" ^
--java-options "javafx.controls,javafx.fxml,javafx.media"

if errorlevel 1 (
    echo ❌ jpackage (app-image) failed!
    pause
    exit /b
)

echo.
echo ✅ Build complete!
echo 🔹 App folder: %DIST_DIR%\%APP_NAME%\
echo 🔹 Executable: %DIST_DIR%\%APP_NAME%\%APP_NAME%.exe
echo ============================================================
pause







