@echo off
setlocal enabledelayedexpansion
title SPARCS Build Tool

echo.
echo =============================================
echo   SPARCS - Build Script
echo =============================================
echo.

:: ── 0. Check correct directory ───────────────────────────────────────────────
if not exist "src\sparcs\SPARCS.java" (
    echo [ERROR] Run this script from your project ROOT folder.
    pause
    exit /b 1
)

:: ── 1. Clean and setup folders ───────────────────────────────────────────────
echo [1/6] Cleaning old build...
if exist "out"           rmdir /s /q "out"
if exist "SPARCS_Submit" rmdir /s /q "SPARCS_Submit"
mkdir out\classes
mkdir SPARCS_Submit
echo      Done.

:: ── 2. Build classpath ───────────────────────────────────────────────────────
echo [2/6] Building classpath...
set CP=
for %%f in (lib\*.jar) do (
    if "!CP!"=="" ( set "CP=%%f" ) else ( set "CP=!CP!;%%f" )
)
if "!CP!"=="" (
    echo [ERROR] No JARs found in lib\
    pause
    exit /b 1
)
echo      Classpath: !CP!

:: ── 3. Compile ───────────────────────────────────────────────────────────────
echo [3/6] Compiling...
dir /s /b src\sparcs\*.java > out\sources.txt
javac --release 21 -cp "!CP!" -sourcepath src\sparcs -d out\classes @out\sources.txt
if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed. See errors above.
    pause
    exit /b 1
)
echo      Compiled OK.

:: ── 4. Extract dependency JARs ───────────────────────────────────────────────
echo [4/6] Merging dependencies...
cd out\classes
for %%f in (..\..\lib\*.jar) do (
    echo      Extracting %%~nxf...
    jar xf "%%~ff"
)
if exist META-INF (
    del /q META-INF\*.SF  2>nul
    del /q META-INF\*.RSA 2>nul
    del /q META-INF\*.DSA 2>nul
)
cd ..\..
echo      Done.

:: ── 5. Copy assets and db.properties ─────────────────────────────────────────
echo [5/6] Copying assets...
if exist "assets"        xcopy /e /i /q assets        out\classes\       >nul
if exist "db.properties" copy  /y      db.properties  out\classes\db.properties >nul
echo      Done.

:: ── 6. Package fat JAR ───────────────────────────────────────────────────────
echo [6/6] Packaging SPARCS.jar...
(
    echo Main-Class: SPARCS
    echo Class-Path: .
    echo.
) > out\MANIFEST.MF

cd out\classes
jar cfm ..\..\SPARCS_Submit\SPARCS.jar ..\MANIFEST.MF .
cd ..\..
if errorlevel 1 (
    echo [ERROR] JAR packaging failed.
    pause
    exit /b 1
)

:: ── 7. Assemble submit folder ─────────────────────────────────────────────────
if exist "db"            xcopy /e /i /q db            SPARCS_Submit\db            >nul
if exist "db.properties" copy  /y       db.properties SPARCS_Submit\db.properties  >nul

(
    echo @echo off
    echo title SPARCS - Smart Parking System
    echo echo Starting SPARCS...
    echo java -jar SPARCS.jar
    echo if errorlevel 1 ^(
    echo     echo [ERROR] Make sure Java 21+ is installed.
    echo     pause
    echo ^)
) > SPARCS_Submit\run.bat

(
    echo @echo off
    echo setlocal enabledelayedexpansion
    echo title SPARCS - Database Setup
    echo echo ============================================
    echo echo   SPARCS - First Time Database Setup
    echo echo ============================================
    echo echo.
    echo echo Make sure MySQL is running first!
    echo echo.
    echo set /p MYSQL_USER=MySQL username ^(default: root^): 
    echo if "%%MYSQL_USER%%"=="" set MYSQL_USER=root
    echo set /p MYSQL_PASS=MySQL password: 
    echo echo Importing...
    echo mysql -u %%MYSQL_USER%% -p%%MYSQL_PASS%% ^< db\sparcs_db.sql
    echo if errorlevel 1 ^(
    echo     echo [ERROR] Import failed. Check credentials.
    echo     pause
    echo     exit /b 1
    echo ^)
    echo echo [SUCCESS] Done! Edit db.properties then run run.bat
    echo pause
) > SPARCS_Submit\SETUP_FIRST.bat

:: ── Done ─────────────────────────────────────────────────────────────────────
echo.
echo =============================================
echo   BUILD COMPLETE!
echo =============================================
echo.
echo   SPARCS_Submit\ is ready:
echo     SPARCS.jar       - the app
echo     run.bat          - launch the app
echo     SETUP_FIRST.bat  - import DB on lab PC
echo     db\              - SQL dump
echo     db.properties    - DB credentials
echo.
echo   TO TEST NOW: open a new terminal and run:
echo     cd SPARCS_Submit
echo     java -jar SPARCS.jar
echo.
pause