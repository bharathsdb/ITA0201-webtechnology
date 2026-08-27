@echo off
echo ====================================================
echo Student Result Processing System - Build and Run
echo ====================================================

REM Ensure lib and classes directories exist
mkdir WEB-INF\lib 2>nul
mkdir WEB-INF\classes 2>nul

echo.
echo [1/3] Checking dependencies...
if not exist "WEB-INF\lib\jakarta.servlet-api-6.0.0.jar" (
    echo Downloading Servlet API...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/jakarta/servlet/jakarta.servlet-api/6.0.0/jakarta.servlet-api-6.0.0.jar' -OutFile 'WEB-INF\lib\jakarta.servlet-api-6.0.0.jar'"
) else (
    echo Servlet API found.
)

echo.
echo [2/3] Compiling Servlet for Tomcat...
javac -cp "WEB-INF\lib\jakarta.servlet-api-6.0.0.jar" -d "WEB-INF\classes" src\StudentResultServlet.java
if %ERRORLEVEL% neq 0 (
    echo Compilation failed! Please check the source code.
    pause
    exit /b %ERRORLEVEL%
)
echo Compilation successful.

echo.
echo [3/3] Starting Local Dev Server...
echo The application will run locally without requiring Tomcat.
echo Press Ctrl+C to stop the server.
echo.
javac DevServer.java
java DevServer
pause
