@echo off
title AI Code Review Assistant - Backend
cd /d "%~dp0"

echo =====================================================================
echo           AI Code Review Assistant - Spring Boot Backend
echo =====================================================================
echo.

:: Configure JAVA_HOME if not already defined
if not defined JAVA_HOME (
    if exist "C:\Program Files\Java\jdk-24" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-24"
    ) else if exist "C:\Program Files\Java\jdk-21" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    )
)
if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

:: Locate Maven executable
where mvn >nul 2>nul
if %ERRORLEVEL% equ 0 (
    set MVN_CMD=mvn
) else if exist "%~dp0..\apache-maven-3.9.9\bin\mvn.cmd" (
    set MVN_CMD="%~dp0..\apache-maven-3.9.9\bin\mvn.cmd"
) else (
    set MVN_CMD=mvn
)

:: Use local in-memory H2 profile by default for instant zero-setup execution
set ACTIVE_PROFILE=local

echo [INFO] Starting Spring Boot with in-memory H2 database (zero-setup profile)...
echo [INFO] Backend will listen on http://localhost:8080
echo [INFO] Swagger UI will be available on http://localhost:8080/swagger-ui.html
echo.
echo Running: %MVN_CMD% spring-boot:run -Dspring-boot.run.profiles=%ACTIVE_PROFILE% -Dmaven.test.skip=true
echo =====================================================================
call %MVN_CMD% spring-boot:run -Dspring-boot.run.profiles=%ACTIVE_PROFILE% -Dmaven.test.skip=true
pause