@echo off
REM ==== Run TicTacToe (Server + Client in one) ====

REM Path to JDK 17
set JAVA_HOME="C:\Program Files\Java\jdk-17"

REM Path to JavaFX SDK
set JAVAFX_HOME="C:\javafx-sdk-17.0.16\lib"

REM Path to compiled classes
set APP_HOME="C:\Games\TicTacToe\out\production\TicTacToe"

REM Path to SQLite JDBC driver
set SQLITE_JDBC="C:\sqlite-jdbc-3.50.3.0.jar"

echo Starting TicTacToe (server + client)...
%JAVA_HOME%\bin\java.exe ^
--module-path %JAVAFX_HOME% ^
--add-modules javafx.controls,javafx.fxml,javafx.media ^
-classpath "%APP_HOME%;%SQLITE_JDBC%" ^
frontEnd.Launcher

pause

