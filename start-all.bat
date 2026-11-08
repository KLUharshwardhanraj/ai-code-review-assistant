@echo off
title AI Code Review Assistant Launcher
cd /d "%~dp0"

echo =====================================================================
echo           Launching AI Code Review Assistant (Full-Stack)
echo =====================================================================
echo.
echo 1. Starting Backend in a new window...
start "AI Code Review - Backend" cmd /c "cd /d "%~dp0ai-code-review-assistant" && call start-backend.bat"

echo 2. Starting Frontend in a new window...
start "AI Code Review - Frontend" cmd /c "cd /d "%~dp0frontend" && call start-frontend.bat"

echo.
echo =====================================================================
echo Both servers have been launched in separate terminal windows!
echo - Backend API:  http://localhost:8080/swagger-ui.html
echo - Frontend UI:  http://localhost:5173
echo =====================================================================
echo.
pause
