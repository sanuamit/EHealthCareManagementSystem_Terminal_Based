@echo off
cd /d "%~dp0"
where javac >nul 2>nul || (echo Install JDK 17+ and add it to PATH.& pause & exit /b 1)
if not exist out mkdir out
javac -encoding UTF-8 -d out src\main\java\com\ehealth\Main.java
if errorlevel 1 (pause & exit /b 1)
java -cp out com.ehealth.Main
pause
