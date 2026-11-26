@echo off
setlocal
cd /d "%~dp0"

title PSP BT Gamepad - Windows Receiver

if not exist ".venv\Scripts\python.exe" (
    echo [ERROR] Python virtual environment not found.
    echo.
    echo Open this folder in Command Prompt and run:
    echo   py -m venv .venv
    echo   .venv\Scripts\python.exe -m pip install -r requirements.txt
    echo.
    pause
    exit /b 1
)

echo Starting PSP BT Gamepad receiver...
echo Keep this window open while playing.
echo.
".venv\Scripts\python.exe" receiver.py

echo.
echo Receiver stopped.
pause
