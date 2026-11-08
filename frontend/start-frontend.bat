@echo off
title AI Code Review Assistant - Frontend
cd /d "%~dp0"

echo =====================================================================
echo           AI Code Review Assistant - React Frontend (Vite)
echo =====================================================================
echo.

:: Check if node_modules exists
if not exist "node_modules\" (
    echo [INFO] First-time setup: installing npm packages...
    call npm install
)

echo.
echo [INFO] Starting Vite development server on http://localhost:5173...
echo =====================================================================
call npm run dev
pause
