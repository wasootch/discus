@echo off
cd /d "%~dp0"

set OUT=out\classes
set MAIN=wasootch.discus.DiskSpaceAnalyzer

if not exist "%OUT%" mkdir "%OUT%"

dir /s /b src\*.java > sources.txt
javac -d "%OUT%" @sources.txt
del sources.txt

if %errorlevel% neq 0 (
    echo Compilation failed.
    pause
    exit /b %errorlevel%
)

java -cp "%OUT%" %MAIN%
pause
