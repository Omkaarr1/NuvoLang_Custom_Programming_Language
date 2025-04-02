@echo off
setlocal enabledelayedexpansion

:: Default script file path
set "SCRIPT_FILE=scripts\example.txt"

:: Allow passing a custom script file as an argument
if not "%~1"=="" (
    set "SCRIPT_FILE=%~1"
)

:: Ensure 'out' directory exists
if not exist out (
    mkdir out
)

echo.
echo 🔧 Compiling Java source files...
javac -d out -cp "lib/*" src\main\java\com\example\lang\*.java

if errorlevel 1 (
    echo ❌ Compilation failed. Aborting.
    exit /b 1
)

echo.
echo 🚀 Running interpreter on: %SCRIPT_FILE%
java -cp "lib/*;out" com.example.lang.Main %SCRIPT_FILE%
