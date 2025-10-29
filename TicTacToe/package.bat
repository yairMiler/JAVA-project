@echo off
REM ==== Build TicTacToe Self-Contained App ====

REM Paths
set JAVA_HOME=C:\Program Files\Java\jdk-17
set JAVAFX_HOME=C:\javafx-sdk-17.0.16\lib
set APP_HOME=C:\Games\TicTacToe\out\production\TicTacToe
set SQLITE_JDBC=C:\sqlite-jdbc-3.50.3.0.jar
set OUTPUT_DIR=C:\Games\TicTacToe\dist

REM Create output folder
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM Step 1: Package client+server classes into JAR
echo.
echo ==== Creating Combined JAR (TicTacToe) ====
"%JAVA_HOME%\bin\jar.exe" cfe "%OUTPUT_DIR%\TicTacToe.jar" frontEnd.Launcher -C "%APP_HOME%" .

REM Step 2: Copy dependencies (SQLite JDBC)
echo.
echo ==== Copying JDBC Driver ====
copy "%SQLITE_JDBC%" "%OUTPUT_DIR%"

REM Step 3: Run jpackage to build self-contained client+server app
echo.
echo ==== Running jpackage (app-image) ====
"%JAVA_HOME%\bin\jpackage.exe" ^
--type app-image ^
--input "%OUTPUT_DIR%" ^
--main-jar TicTacToe.jar ^
--main-class frontEnd.Launcher ^
--name "TicTacToe" ^
--icon "C:\Games\TicTacToe\icon.ico" ^
--java-options "--module-path" ^
--java-options "%JAVAFX_HOME%" ^
--java-options "--add-modules" ^
--java-options "javafx.controls,javafx.fxml,javafx.media"

echo.
echo ==== Build Complete! Check the dist\TicTacToe folder for runnable app ====
pause




